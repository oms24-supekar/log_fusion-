package com.ntro.ulpf.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

public record LogRequest(

        @NotBlank(message = "Raw log content cannot be empty")
        String rawContent,

        @NotBlank(message = "Source name cannot be empty")
        String sourceName,

        String sourceType,

        LocalDateTime receivedAt,

        UUID batchId
) {

    /*
     * Normal single-log request.
     */
    public LogRequest(
            String rawContent,
            String sourceName,
            String sourceType
    ) {
        this(
                rawContent,
                sourceName,
                sourceType,
                LocalDateTime.now(),
                null
        );
    }

    /*
     * Existing/backdated request support.
     *
     * Keeps old tests and existing code working.
     */
    public LogRequest(
            String rawContent,
            String sourceName,
            String sourceType,
            LocalDateTime receivedAt
    ) {
        this(
                rawContent,
                sourceName,
                sourceType,
                receivedAt,
                null
        );
    }

    /*
     * Batch-processing request.
     */
    public LogRequest(
            String rawContent,
            String sourceName,
            String sourceType,
            UUID batchId
    ) {
        this(
                rawContent,
                sourceName,
                sourceType,
                LocalDateTime.now(),
                batchId
        );
    }
}