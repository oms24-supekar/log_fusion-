package com.ntro.ulpf.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LogResponse(

        UUID id,
        String sourceName,
        String sourceType,
        String detectedFormat,
        String sha256Hash,
        String processingStatus,
        LocalDateTime receivedAt
) {
}