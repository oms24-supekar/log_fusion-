package com.ntro.ulpf.service;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ntro.ulpf.dto.LogRequest;
import com.ntro.ulpf.dto.LogResponse;
import com.ntro.ulpf.entity.BatchJob;
import com.ntro.ulpf.repository.BatchJobRepository;

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

            batch.setStatus(
                    "INGESTING"
            );

            batch.setLastUpdatedAt(
                    LocalDateTime.now()
            );

            batchJobRepository.save(
                    batch
            );

            Path filePath =
                    Path.of(
                            batch.getStoragePath()
                    );

            /*
             * Stream the file instead of loading
             * the whole batch into memory.
             */
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

                    chunk.add(
                            trimmed
                    );

                    if (chunk.size()
                            >= CHUNK_SIZE) {

                        processChunk(
                                batchId,
                                chunk
                        );

                        chunk.clear();
                    }
                }

                if (!chunk.isEmpty()) {

                    processChunk(
                            batchId,
                            chunk
                    );
                }
            }

            /*
             * IMPORTANT:
             *
             * Reload from PostgreSQL.
             *
             * Kafka consumers may already have updated
             * AI counters while this batch was ingesting.
             *
             * Never save the old/stale BatchJob object here.
             */
            BatchJob latestBatch =
                    batchJobRepository
                            .findById(batchId)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Batch disappeared: "
                                                    + batchId
                                    )
                            );

            boolean aiRemaining =
                    latestBatch.getAiQueued() > 0
                            ||
                    latestBatch.getAiProcessing() > 0;

            long handled =
                    latestBatch.getNormalizedLogs()
                            +
                    latestBatch.getFailedLogs();

            boolean allHandled =
                    latestBatch.getTotalLogs() > 0
                            &&
                    handled >=
                            latestBatch.getTotalLogs();

            if (aiRemaining) {

                latestBatch.setStatus(
                        "AI_DRAINING"
                );

            } else if (allHandled) {

                if (latestBatch.getFailedLogs() > 0) {

                    latestBatch.setStatus(
                            "COMPLETED_WITH_ERRORS"
                    );

                } else {

                    latestBatch.setStatus(
                            "COMPLETED"
                    );
                }

                latestBatch.setCompletedAt(
                        LocalDateTime.now()
                );

            } else {

                latestBatch.setStatus(
                        "PROCESSING"
                );
            }

            latestBatch.setLastUpdatedAt(
                    LocalDateTime.now()
            );

            batchJobRepository.save(
                    latestBatch
            );

        } catch (Exception e) {

            /*
             * Reload the newest row before marking failure.
             * Avoid overwriting counters modified by Kafka.
             */
            batchJobRepository
                    .findById(batchId)
                    .ifPresent(latestBatch -> {

                        latestBatch.setStatus(
                                "FAILED"
                        );

                        latestBatch.setLastUpdatedAt(
                                LocalDateTime.now()
                        );

                        batchJobRepository.save(
                                latestBatch
                        );
                    });

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
            List<String> chunk
    ) {

        /*
         * These counters belong only to the synchronous
         * deterministic ingestion side.
         *
         * AI counters are updated atomically by
         * BatchProgressService.
         */
        long accepted = 0;
        long deterministic = 0;
        long normalized = 0;
        long failed = 0;

        BatchJob batch =
                batchJobRepository
                        .findById(batchId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Batch not found: "
                                                + batchId
                                )
                        );

        for (String rawContent
                : chunk) {

            try {

                LogRequest request =
                        new LogRequest(
                                rawContent,
                                batch.getSourceName(),
                                batch.getSourceType(),
                                batchId
                        );

                LogResponse response =
                        rawLogService
                                .saveRawLog(
                                        request
                                );

                accepted++;

                String status =
                        response.processingStatus();

                /*
                 * Deterministic normalization completed
                 * synchronously.
                 */
                if ("NORMALIZED".equals(
                        status
                )) {

                    deterministic++;

                    normalized++;
                }

                /*
                 * Unknown format was handed to Kafka.
                 *
                 * Do NOT modify the BatchJob Java object.
                 *
                 * Update PostgreSQL atomically.
                 */
                else if ("AI_QUEUED".equals(
                        status
                )) {

                    batchProgressService
                            .markAiQueued(
                                    batchId
                            );
                }

                /*
                 * Usually unlikely to be returned directly
                 * because AI processing is asynchronous,
                 * but handle it safely if it occurs.
                 */
                else if ("AI_NORMALIZING".equals(
                        status
                )) {

                    /*
                     * The AI consumer has already started
                     * processing this job.
                     *
                     * Do not increment aiQueued here.
                     */
                }

                else if (
                        status != null
                                &&
                        status.contains(
                                "FAILED"
                        )
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
         * Reload the latest batch state because
         * Kafka workers may have modified AI counters
         * during this chunk.
         */
        BatchJob latestBatch =
                batchJobRepository
                        .findById(batchId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Batch not found: "
                                                + batchId
                                )
                        );

        latestBatch.setTotalLogs(
                latestBatch.getTotalLogs()
                        + chunk.size()
        );

        latestBatch.setAcceptedLogs(
                latestBatch.getAcceptedLogs()
                        + accepted
        );

        latestBatch.setDeterministicProcessed(
                latestBatch.getDeterministicProcessed()
                        + deterministic
        );

        latestBatch.setNormalizedLogs(
                latestBatch.getNormalizedLogs()
                        + normalized
        );

        latestBatch.setFailedLogs(
                latestBatch.getFailedLogs()
                        + failed
        );

        /*
         * Do not blindly set PROCESSING if Kafka
         * has already moved the batch to AI_DRAINING.
         */
        if (
                latestBatch.getAiQueued() > 0
                        ||
                latestBatch.getAiProcessing() > 0
        ) {

            latestBatch.setStatus(
                    "AI_DRAINING"
            );

        } else {

            latestBatch.setStatus(
                    "PROCESSING"
            );
        }

        latestBatch.setLastUpdatedAt(
                LocalDateTime.now()
        );

        batchJobRepository.save(
                latestBatch
        );

        System.out.println(
                "Batch "
                        + batchId
                        + " processed "
                        + latestBatch.getTotalLogs()
                        + " logs"
        );
    }
}