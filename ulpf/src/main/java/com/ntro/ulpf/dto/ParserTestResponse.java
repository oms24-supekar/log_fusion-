package com.ntro.ulpf.dto;

import java.util.Map;

public record ParserTestResponse(
        String detectedFormat,
        Map<String, Object> parsedFields
) {
}