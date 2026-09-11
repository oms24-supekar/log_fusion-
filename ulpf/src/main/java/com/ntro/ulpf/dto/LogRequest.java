package com.ntro.ulpf.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record LogRequest(

        @NotBlank(message = "Raw log content cannot be empty")
        String rawContent,

        @NotBlank(message = "Source name cannot be empty")
        String sourceName,

        String sourceType,

        LocalDateTime receivedAt
) {

    public LogRequest(
            String rawContent,
            String sourceName,
            String sourceType
    ) {
        this(rawContent, sourceName, sourceType, LocalDateTime.now());
    }
}