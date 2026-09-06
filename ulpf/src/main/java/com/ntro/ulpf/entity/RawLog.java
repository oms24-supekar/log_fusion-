package com.ntro.ulpf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "raw_logs")
public class RawLog {

    @Id
    private UUID id;

    @Column(name = "raw_content", nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "detected_format")
    private String detectedFormat;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processing_status", nullable = false)
    private String processingStatus;

    public RawLog() {
    }

    public RawLog(
            UUID id,
            String rawContent,
            String sourceName,
            String sourceType,
            String detectedFormat,
            String sha256Hash,
            LocalDateTime receivedAt,
            String processingStatus
    ) {
        this.id = id;
        this.rawContent = rawContent;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.detectedFormat = detectedFormat;
        this.sha256Hash = sha256Hash;
        this.receivedAt = receivedAt;
        this.processingStatus = processingStatus;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDetectedFormat() {
        return detectedFormat;
    }

    public void setDetectedFormat(String detectedFormat) {
        this.detectedFormat = detectedFormat;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }
}