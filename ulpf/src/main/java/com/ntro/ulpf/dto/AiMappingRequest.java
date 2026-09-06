package com.ntro.ulpf.dto;

import java.util.List;

public record AiMappingRequest(
        String raw_log,
        List<AiFieldCandidate> extracted_fields
) {
}