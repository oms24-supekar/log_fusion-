package com.ntro.ulpf.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BatchAcceptedResponse(
        UUID batchId,
        String fileName,
        String status,
        LocalDateTime acceptedAt
) {
}
