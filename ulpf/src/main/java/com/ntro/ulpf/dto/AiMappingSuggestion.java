package com.ntro.ulpf.dto;

public record AiMappingSuggestion(
        String source_field,
        String sample_value,
        String suggested_universal_field,
        double confidence,
        String method
) {
}