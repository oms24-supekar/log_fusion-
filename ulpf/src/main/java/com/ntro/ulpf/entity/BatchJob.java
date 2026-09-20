package com.ntro.ulpf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_jobs")
public class BatchJob {

    @Id
    private UUID id;

    @Column(
            name = "file_name",
            nullable = false
    )
    private String fileName;

    @Column(
            name = "source_name"
    )
    private String sourceName;

    @Column(
            name = "source_type"
    )
    private String sourceType;

    @Column(
            name = "status",
            nullable = false
    )
    private String status;

    @Column(
            name = "total_logs",
            nullable = false
    )
    private long totalLogs;

    @Column(
            name = "accepted_logs",
            nullable = false
    )
    private long acceptedLogs;

    @Column(
            name = "deterministic_processed",
            nullable = false
    )
    private long deterministicProcessed;

    @Column(
            name = "ai_queued",
            nullable = false
    )
    private long aiQueued;

    @Column(
            name = "ai_processing",
            nullable = false
    )
    private long aiProcessing;

    @Column(
            name = "normalized_logs",
            nullable = false
    )
    private long normalizedLogs;

    @Column(
            name = "failed_logs",
            nullable = false
    )
    private long failedLogs;

    @Column(
            name = "started_at",
            nullable = false
    )
    private LocalDateTime startedAt;

    @Column(
            name = "completed_at"
    )
    private LocalDateTime completedAt;

    @Column(
            name = "last_updated_at",
            nullable = false
    )
    private LocalDateTime lastUpdatedAt;
    @Column(
        name = "storage_path",
        nullable = false
)
private String storagePath;

    public BatchJob() {
    }

   public BatchJob(
        UUID id,
        String fileName,
        String storagePath,
        String sourceName,
        String sourceType,
        String status,
        long totalLogs,
        long acceptedLogs,
        long deterministicProcessed,
        long aiQueued,
        long aiProcessing,
        long normalizedLogs,
        long failedLogs,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime lastUpdatedAt
) {
        this.id = id;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.status = status;
        this.totalLogs = totalLogs;
        this.acceptedLogs = acceptedLogs;
        this.deterministicProcessed = deterministicProcessed;
        this.aiQueued = aiQueued;
        this.aiProcessing = aiProcessing;
        this.normalizedLogs = normalizedLogs;
        this.failedLogs = failedLogs;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTotalLogs() {
        return totalLogs;
    }

    public void setTotalLogs(long totalLogs) {
        this.totalLogs = totalLogs;
    }

    public long getAcceptedLogs() {
        return acceptedLogs;
    }

    public void setAcceptedLogs(long acceptedLogs) {
        this.acceptedLogs = acceptedLogs;
    }

    public long getDeterministicProcessed() {
        return deterministicProcessed;
    }

    public void setDeterministicProcessed(
            long deterministicProcessed
    ) {
        this.deterministicProcessed =
                deterministicProcessed;
    }

    public long getAiQueued() {
        return aiQueued;
    }

    public void setAiQueued(long aiQueued) {
        this.aiQueued = aiQueued;
    }

    public long getAiProcessing() {
        return aiProcessing;
    }

    public void setAiProcessing(long aiProcessing) {
        this.aiProcessing = aiProcessing;
    }

    public long getNormalizedLogs() {
        return normalizedLogs;
    }

    public void setNormalizedLogs(
            long normalizedLogs
    ) {
        this.normalizedLogs =
                normalizedLogs;
    }

    public long getFailedLogs() {
        return failedLogs;
    }

    public void setFailedLogs(long failedLogs) {
        this.failedLogs = failedLogs;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(
            LocalDateTime startedAt
    ) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(
            LocalDateTime completedAt
    ) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(
            LocalDateTime lastUpdatedAt
    ) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}