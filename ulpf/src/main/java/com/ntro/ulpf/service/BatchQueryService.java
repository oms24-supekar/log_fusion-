package com.ntro.ulpf.service;

import com.ntro.ulpf.dto.BatchLogResponse;
import com.ntro.ulpf.dto.BatchLogsPageResponse;
import com.ntro.ulpf.dto.BatchProgressResponse;
import com.ntro.ulpf.entity.BatchJob;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.repository.BatchJobRepository;
import com.ntro.ulpf.repository.RawLogRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BatchQueryService {

    private static final int MAX_PAGE_SIZE = 500;

    private final BatchJobRepository batchJobRepository;
    private final RawLogRepository rawLogRepository;

    public BatchQueryService(
            BatchJobRepository batchJobRepository,
            RawLogRepository rawLogRepository
    ) {
        this.batchJobRepository = batchJobRepository;
        this.rawLogRepository = rawLogRepository;
    }

    public List<BatchProgressResponse> getAllBatches() {
        return batchJobRepository
                .findAllByOrderByStartedAtDesc()
                .stream()
                .map(this::toProgressResponse)
                .toList();
    }

    public BatchProgressResponse getBatch(
            UUID batchId
    ) {
        BatchJob batch =
                batchJobRepository
                        .findById(batchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Batch not found: " + batchId
                                )
                        );

        return toProgressResponse(batch);
    }

    public BatchLogsPageResponse getBatchLogs(
            UUID batchId,
            int page,
            int size
    ) {
        /*
         * Validate batch first so an unknown batch does not
         * silently return an empty page.
         */
        if (!batchJobRepository.existsById(batchId)) {
            throw new IllegalArgumentException(
                    "Batch not found: " + batchId
            );
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(
                Math.max(size, 1),
                MAX_PAGE_SIZE
        );

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize
                );

        Page<RawLog> result =
                rawLogRepository
                        .findByBatchIdOrderByReceivedAtAsc(
                                batchId,
                                pageable
                        );

        List<BatchLogResponse> logs =
                result.getContent()
                        .stream()
                        .map(this::toLogResponse)
                        .toList();

        return new BatchLogsPageResponse(
                batchId,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast(),
                logs
        );
    }

    private BatchProgressResponse toProgressResponse(
            BatchJob batch
    ) {
        long handled =
                batch.getNormalizedLogs()
                        + batch.getFailedLogs();

        long pending =
                Math.max(
                        batch.getTotalLogs() - handled,
                        0
                );

        double progress =
                batch.getTotalLogs() <= 0
                        ? 0.0
                        : Math.min(
                                100.0,
                                ((double) handled
                                        / batch.getTotalLogs())
                                        * 100.0
                        );

        /*
         * Keep API output stable/readable.
         */
        progress =
                Math.round(progress * 100.0)
                        / 100.0;

        return new BatchProgressResponse(
                batch.getId(),
                batch.getFileName(),
                batch.getSourceName(),
                batch.getSourceType(),
                batch.getStatus(),
                batch.getTotalLogs(),
                batch.getAcceptedLogs(),
                batch.getDeterministicProcessed(),
                batch.getAiQueued(),
                batch.getAiProcessing(),
                batch.getNormalizedLogs(),
                batch.getFailedLogs(),
                pending,
                progress,
                batch.getStartedAt(),
                batch.getCompletedAt(),
                batch.getLastUpdatedAt()
        );
    }

    private BatchLogResponse toLogResponse(
            RawLog rawLog
    ) {
        return new BatchLogResponse(
                rawLog.getId(),
                rawLog.getBatchId(),
                rawLog.getSourceName(),
                rawLog.getSourceType(),
                rawLog.getDetectedFormat(),
                rawLog.getProcessingStatus(),
                rawLog.getSha256Hash(),
                rawLog.getReceivedAt()
        );
    }
}
