package com.ntro.ulpf.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record NormalizedLogDetailsResponse(
        UUID id,
        JsonNode data,
        String parserUsed,
        String validationStatus,
        LocalDateTime processedAt
) {
}