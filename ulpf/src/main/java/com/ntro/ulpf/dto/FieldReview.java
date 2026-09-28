package com.ntro.ulpf.dto;

public record FieldReview(
        String field,
        double confidence,
        boolean suspicious,
        String reason
) {
}