package com.ntro.ulpf.dto;

public record FieldMappingSuggestion(
        String sourceField,
        String sampleValue,
        String suggestedUniversalField,
        double confidence,
        String reason
) {
}