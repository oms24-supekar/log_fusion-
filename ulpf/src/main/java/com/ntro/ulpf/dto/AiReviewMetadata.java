package com.ntro.ulpf.dto;

import java.util.List;

public record AiReviewMetadata(
        double overallConfidence,
        List<FieldReview> fields
) {
}