package com.ntro.ulpf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "normalized_logs")
public class NormalizedLog {

    @Id
    private UUID id;

    @OneToOne
    @JoinColumn(
            name = "raw_log_id",
            nullable = false,
            unique = true
    )
    private RawLog rawLog;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "normalized_data",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String normalizedData;

    @Column(
            name = "parser_used",
            nullable = false
    )
    private String parserUsed;

    @Column(
            name = "processed_at",
            nullable = false
    )
    private LocalDateTime processedAt;

    @Column(
            name = "validation_status",
            nullable = false
    )
    private String validationStatus;

    public NormalizedLog() {
    }

    public NormalizedLog(
            UUID id,
            RawLog rawLog,
            String normalizedData,
            String parserUsed,
            LocalDateTime processedAt,
            String validationStatus
    ) {
        this.id = id;
        this.rawLog = rawLog;
        this.normalizedData = normalizedData;
        this.parserUsed = parserUsed;
        this.processedAt = processedAt;
        this.validationStatus = validationStatus;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RawLog getRawLog() {
        return rawLog;
    }

    public void setRawLog(RawLog rawLog) {
        this.rawLog = rawLog;
    }

    public String getNormalizedData() {
        return normalizedData;
    }

    public void setNormalizedData(String normalizedData) {
        this.normalizedData = normalizedData;
    }

    public String getParserUsed() {
        return parserUsed;
    }

    public void setParserUsed(String parserUsed) {
        this.parserUsed = parserUsed;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }
}