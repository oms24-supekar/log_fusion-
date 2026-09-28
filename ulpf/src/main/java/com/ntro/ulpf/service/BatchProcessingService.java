package com.ntro.ulpf.service;

import com.ntro.ulpf.dto.LogRequest;
import com.ntro.ulpf.dto.LogResponse;
import com.ntro.ulpf.entity.BatchJob;
import com.ntro.ulpf.repository.BatchJobRepository;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BatchProcessingService {

    private static final int CHUNK_SIZE = 500;

    private final BatchJobRepository batchJobRepository;
    private final RawLogService rawLogService;
    private final BatchProgressService batchProgressService;

    public BatchProcessingService(
            BatchJobRepository batchJobRepository,
            RawLogService rawLogService,
            BatchProgressService batchProgressService
    ) {
        this.batchJobRepository =
                batchJobRepository;

        this.rawLogService =
                rawLogService;

        this.batchProgressService =
                batchProgressService;
    }

    @Async("batchExecutor")
    public void process(
            UUID batchId
    ) {

        /*
         * Load static batch metadata once.
         *
         * We DO NOT use this entity to update counters.
         */
        BatchJob batch =
                batchJobRepository
                        .findById(batchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Batch not found: "
                                                + batchId
                                )
                        );

        try {

            batchJobRepository.markIngestionStarted(
                    batchId,
                    LocalDateTime.now()
            );

            Path filePath =
                    Path.of(
                            batch.getStoragePath()
                    );

            try (
                    BufferedReader reader =
                            Files.newBufferedReader(
                                    filePath,
                                    StandardCharsets.UTF_8
                            )
            ) {

                List<String> chunk =
                        new ArrayList<>(
                                CHUNK_SIZE
                        );

                String line;

                while (
                        (line = reader.readLine())
                                != null
                ) {

                    String trimmed =
                            line.trim();

                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    chunk.add(trimmed);

                    if (chunk.size()
                            >= CHUNK_SIZE) {

                        processChunk(
                                batchId,
                                batch.getSourceName(),
                                batch.getSourceType(),
                                chunk
                        );

                        chunk.clear();
                    }
                }

                if (!chunk.isEmpty()) {

                    processChunk(
                            batchId,
                            batch.getSourceName(),
                            batch.getSourceType(),
                            chunk
                    );
                }
            }

            /*
             * Database decides PROCESSING / AI_DRAINING /
             * COMPLETED / COMPLETED_WITH_ERRORS.
             */
            batchProgressService
                    .refreshBatchStatus(
                            batchId
                    );

        } catch (Exception e) {

            batchJobRepository.markBatchFailed(
                    batchId,
                    LocalDateTime.now()
            );

            System.err.println(
                    "Batch processing failed for "
                            + batchId
                            + ": "
                            + e.getMessage()
            );
        }
    }

    private void processChunk(
            UUID batchId,
            String sourceName,
            String sourceType,
            List<String> chunk
    ) {

        long accepted = 0;
        long deterministic = 0;
        long normalized = 0;
        long failed = 0;

        for (String rawContent
                : chunk) {

            try {

                LogRequest request =
                        new LogRequest(
                                rawContent,
                                sourceName,
                                sourceType,
                                batchId
                        );

                LogResponse response =
                        rawLogService.saveRawLog(
                                request
                        );

                accepted++;

                String status =
                        response.processingStatus();

                /*
                 * Only synchronous deterministic work is counted here.
                 *
                 * AI queue counters are owned by the AI queueing path
                 * BEFORE Kafka publish. Never increment aiQueued here.
                 */
                if ("NORMALIZED".equals(status)) {

                    deterministic++;
                    normalized++;

                } else if (
                        status != null
                        && status.contains("FAILED")
                ) {

                    failed++;
                }

            } catch (Exception e) {

                failed++;

                System.err.println(
                        "Batch log failed: "
                                + e.getMessage()
                );
            }
        }

        /*
         * One atomic counter UPDATE per 500-log chunk.
         */
        batchJobRepository.addChunkCounters(
                batchId,
                chunk.size(),
                accepted,
                deterministic,
                normalized,
                failed,
                LocalDateTime.now()
        );

        batchProgressService
                .refreshBatchStatus(
                        batchId
                );

        System.out.println(
                "Batch "
                        + batchId
                        + " processed another "
                        + chunk.size()
                        + " logs"
        );
    }
}
