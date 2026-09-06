package com.ntro.ulpf.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ParserDefinitionResponse(
        UUID id,
        String name,
        String signaturePrefix,
        String delimiter,
        String keyValueSeparator,
        Map<String, String> fieldMappings,
        boolean enabled,
        LocalDateTime createdAt
) {
}