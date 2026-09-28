package com.ntro.ulpf.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parser_learning_candidates")
public class ParserLearningCandidate {

    @Id
    private UUID id;

    /*
     * Stable structural identity.
     *
     * Similar logs should produce the same fingerprint.
     */
    @Column(
            name = "fingerprint",
            nullable = false,
            unique = true,
            length = 64
    )
    private String fingerprint;

    /*
     * Human-readable structural signature.
     *
     * Example:
     * MYSTERY_EVT@@
     * ZX9!
     * RAVEN#
     */
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

    /*
     * AI-learned mapping:
     *
     * usr -> username
     * addr -> source_ip
     * auth -> outcome
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "field_mappings",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String fieldMappings;

    /*
     * Number of structurally similar logs observed.
     */
    @Column(
            name = "examples_seen",
            nullable = false
    )
    private long examplesSeen;

    /*
     * Number successfully normalized by AI.
     */
    @Column(
            name = "successful_examples",
            nullable = false
    )
    private long successfulExamples;

    /*
     * Number where the structure/mapping conflicted
     * with previous examples.
     */
    @Column(
            name = "conflicting_examples",
            nullable = false
    )
    private long conflictingExamples;

    @Column(
            name = "confidence",
            nullable = false
    )
    private double confidence;

    /*
     * LEARNING
     * READY
     * PROMOTED
     * REJECTED
     */
    @Column(
            name = "status",
            nullable = false
    )
    private String status;

    /*
     * ParserDefinition UUID once promoted.
     */
    @Column(
            name = "promoted_parser_id"
    )
    private UUID promotedParserId;

    /*
     * Keep one representative raw example.
     *
     * Useful when testing the generated parser.
     */
    @Column(
            name = "sample_raw_log",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String sampleRawLog;

    @Column(
            name = "first_seen_at",
            nullable = false
    )
    private LocalDateTime firstSeenAt;

    @Column(
            name = "last_seen_at",
            nullable = false
    )
    private LocalDateTime lastSeenAt;

    public ParserLearningCandidate() {
    }

    public ParserLearningCandidate(
            UUID id,
            String fingerprint,
            String signaturePrefix,
            String delimiter,
            String keyValueSeparator,
            String fieldMappings,
            long examplesSeen,
            long successfulExamples,
            long conflictingExamples,
            double confidence,
            String status,
            UUID promotedParserId,
            String sampleRawLog,
            LocalDateTime firstSeenAt,
            LocalDateTime lastSeenAt
    ) {
        this.id = id;
        this.fingerprint = fingerprint;
        this.signaturePrefix = signaturePrefix;
        this.delimiter = delimiter;
        this.keyValueSeparator = keyValueSeparator;
        this.fieldMappings = fieldMappings;
        this.examplesSeen = examplesSeen;
        this.successfulExamples = successfulExamples;
        this.conflictingExamples = conflictingExamples;
        this.confidence = confidence;
        this.status = status;
        this.promotedParserId = promotedParserId;
        this.sampleRawLog = sampleRawLog;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(
            String fingerprint
    ) {
        this.fingerprint = fingerprint;
    }

    public String getSignaturePrefix() {
        return signaturePrefix;
    }

    public void setSignaturePrefix(
            String signaturePrefix
    ) {
        this.signaturePrefix = signaturePrefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(
            String delimiter
    ) {
        this.delimiter = delimiter;
    }

    public String getKeyValueSeparator() {
        return keyValueSeparator;
    }

    public void setKeyValueSeparator(
            String keyValueSeparator
    ) {
        this.keyValueSeparator =
                keyValueSeparator;
    }

    public String getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(
            String fieldMappings
    ) {
        this.fieldMappings =
                fieldMappings;
    }

    public long getExamplesSeen() {
        return examplesSeen;
    }

    public void setExamplesSeen(
            long examplesSeen
    ) {
        this.examplesSeen =
                examplesSeen;
    }

    public long getSuccessfulExamples() {
        return successfulExamples;
    }

    public void setSuccessfulExamples(
            long successfulExamples
    ) {
        this.successfulExamples =
                successfulExamples;
    }

    public long getConflictingExamples() {
        return conflictingExamples;
    }

    public void setConflictingExamples(
            long conflictingExamples
    ) {
        this.conflictingExamples =
                conflictingExamples;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(
            double confidence
    ) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status
    ) {
        this.status = status;
    }

    public UUID getPromotedParserId() {
        return promotedParserId;
    }

    public void setPromotedParserId(
            UUID promotedParserId
    ) {
        this.promotedParserId =
                promotedParserId;
    }

    public String getSampleRawLog() {
        return sampleRawLog;
    }

    public void setSampleRawLog(
            String sampleRawLog
    ) {
        this.sampleRawLog =
                sampleRawLog;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(
            LocalDateTime firstSeenAt
    ) {
        this.firstSeenAt =
                firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(
            LocalDateTime lastSeenAt
    ) {
        this.lastSeenAt =
                lastSeenAt;
    }
}