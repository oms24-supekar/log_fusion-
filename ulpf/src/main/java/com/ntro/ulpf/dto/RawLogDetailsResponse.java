package com.ntro.ulpf.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RawLogDetailsResponse(
        UUID id,
        String content,
        String sourceName,
        String sourceType,
        String detectedFormat,
        String sha256Hash,
        String processingStatus,
        LocalDateTime receivedAt
) {
}