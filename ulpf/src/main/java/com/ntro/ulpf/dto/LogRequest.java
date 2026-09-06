package com.ntro.ulpf.dto;

import jakarta.validation.constraints.NotBlank;

public record LogRequest(

        @NotBlank(message = "Raw log content cannot be empty")
        String rawContent,

        @NotBlank(message = "Source name cannot be empty")
        String sourceName,

        String sourceType
) {
}