package com.ntro.ulpf.dto;

import java.util.List;

public record AiMappingResponse(
        List<AiMappingSuggestion> suggestions
) {
}