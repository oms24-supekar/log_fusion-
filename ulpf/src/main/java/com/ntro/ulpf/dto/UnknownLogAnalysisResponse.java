package com.ntro.ulpf.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UnknownLogAnalysisResponse(
        UUID rawLogId,
        String rawContent,
        String detectedDelimiter,
        String detectedKeyValueSeparator,
        String suggestedSignaturePrefix,
        Map<String, Object> extractedFields,

        List<FieldMappingSuggestion> deterministicSuggestions,

        List<AiMappingSuggestion> aiSuggestions,

        String aiStatus
) {
}