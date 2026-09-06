package com.ntro.ulpf.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed representation of a normalized log event returned to the frontend.
 * The normalized event is exposed as a regular map so Spring serializes the
 * actual universal-schema JSON rather than Jackson JsonNode metadata.
 */
public record NormalizedLogDetailsResponse(
        UUID id,
        Map<String, Object> data,
        String parserUsed,
        String validationStatus,
        LocalDateTime processedAt
) {
}