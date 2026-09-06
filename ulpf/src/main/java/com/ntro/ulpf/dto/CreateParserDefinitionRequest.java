package com.ntro.ulpf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record CreateParserDefinitionRequest(

        @NotBlank(message = "Parser name is required")
        String name,

        @NotBlank(message = "Signature prefix is required")
        String signaturePrefix,

        @NotBlank(message = "Delimiter is required")
        String delimiter,

        @NotBlank(message = "Key-value separator is required")
        String keyValueSeparator,

        @NotEmpty(message = "At least one field mapping is required")
        Map<String, String> fieldMappings,

        boolean enabled
) {
}