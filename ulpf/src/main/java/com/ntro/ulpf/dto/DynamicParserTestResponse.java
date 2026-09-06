package com.ntro.ulpf.dto;

import java.util.Map;
import java.util.UUID;

public record DynamicParserTestResponse(
        UUID parserDefinitionId,
        String parserName,
        String detectedFormat,
        Map<String, Object> parsedFields
) {
}