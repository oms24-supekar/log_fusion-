package com.ntro.ulpf.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BatchProgressResponse(
        UUID batchId,
        String fileName,
        String sourceName,
        String sourceType,
        String status,
        long totalLogs,
        long acceptedLogs,
        long deterministicProcessed,
        long aiQueued,
        long aiProcessing,
        long normalizedLogs,
        long failedLogs,
        long pendingLogs,
        double progress,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime lastUpdatedAt
) {
}
