package com.ntro.ulpf.kafka;

import java.time.LocalDateTime;
import java.util.UUID;

public record AiLogJob(
        UUID rawLogId,
        String sourceName,
        String sourceType,
        String detectedFormat,
        String sha256Hash,
        String rawContent,
        double deterministicConfidence,
        int attempt,
        LocalDateTime receivedAt,
        LocalDateTime requestedAt
) {
}