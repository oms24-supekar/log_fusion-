package com.ntro.ulpf.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LogSummaryResponse(
        UUID id,
        String sourceName,
        String sourceType,
        String detectedFormat,
        String processingStatus,
        String sha256Hash,
        LocalDateTime receivedAt
) {
}