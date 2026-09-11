package com.ntro.ulpf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parser_definitions")
public class ParserDefinition {

    @Id
    private UUID id;

    @Column(
            name = "name",
            nullable = false,
            unique = true
    )
    private String name;

    @Column(
            name = "signature_prefix",
            nullable = false
    )
    private String signaturePrefix;

    @Column(
            name = "delimiter_value",
            nullable = false
    )
    private String delimiter;

    @Column(
            name = "key_value_separator",
            nullable = false
    )
    private String keyValueSeparator;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "field_mappings",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String fieldMappings;

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled;

    @Column(
            name = "confidence",
            nullable = true
    )
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "creation_mode",
            nullable = false
    )
    private ParserCreationMode creationMode = ParserCreationMode.MANUAL;

    @Column(
            name = "successful_matches",
            nullable = false
    )
    private long successfulMatches;

    @Column(
            name = "failed_matches",
            nullable = false
    )
    private long failedMatches;

    @Column(
            name = "last_matched_at"
    )
    private LocalDateTime lastMatchedAt;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    public ParserDefinition() {
    }

    public ParserDefinition(
            UUID id,
            String name,
            String signaturePrefix,
            String delimiter,
            String keyValueSeparator,
            String fieldMappings,
            boolean enabled,
            LocalDateTime createdAt
    ) {
        this(
                id,
                name,
                signaturePrefix,
                delimiter,
                keyValueSeparator,
                fieldMappings,
                enabled,
                null,
                ParserCreationMode.MANUAL,
                0,
                0,
                null,
                createdAt
        );
    }

    public ParserDefinition(
            UUID id,
            String name,
            String signaturePrefix,
            String delimiter,
            String keyValueSeparator,
            String fieldMappings,
            boolean enabled,
            Double confidence,
            ParserCreationMode creationMode,
            long successfulMatches,
            long failedMatches,
            LocalDateTime lastMatchedAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.name = name;
        this.signaturePrefix = signaturePrefix;
        this.delimiter = delimiter;
        this.keyValueSeparator = keyValueSeparator;
        this.fieldMappings = fieldMappings;
        this.enabled = enabled;
        this.confidence = confidence;
        this.creationMode = creationMode == null ? ParserCreationMode.MANUAL : creationMode;
        this.successfulMatches = successfulMatches;
        this.failedMatches = failedMatches;
        this.lastMatchedAt = lastMatchedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSignaturePrefix() {
        return signaturePrefix;
    }

    public void setSignaturePrefix(String signaturePrefix) {
        this.signaturePrefix = signaturePrefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getKeyValueSeparator() {
        return keyValueSeparator;
    }

    public void setKeyValueSeparator(String keyValueSeparator) {
        this.keyValueSeparator = keyValueSeparator;
    }

    public String getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(String fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public ParserCreationMode getCreationMode() {
        return creationMode;
    }

    public void setCreationMode(ParserCreationMode creationMode) {
        this.creationMode = creationMode;
    }

    public long getSuccessfulMatches() {
        return successfulMatches;
    }

    public void setSuccessfulMatches(long successfulMatches) {
        this.successfulMatches = successfulMatches;
    }

    public long getFailedMatches() {
        return failedMatches;
    }

    public void setFailedMatches(long failedMatches) {
        this.failedMatches = failedMatches;
    }

    public LocalDateTime getLastMatchedAt() {
        return lastMatchedAt;
    }

    public void setLastMatchedAt(LocalDateTime lastMatchedAt) {
        this.lastMatchedAt = lastMatchedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}