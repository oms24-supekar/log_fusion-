package com.ntro.ulpf.repository;

import com.ntro.ulpf.entity.BatchJob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BatchJobRepository
        extends JpaRepository<BatchJob, UUID> {

    List<BatchJob> findAllByOrderByStartedAtDesc();

    List<BatchJob> findByStatusOrderByStartedAtDesc(
            String status
    );

    /*
     * ---------------------------------------------------------
     * STATUS-ONLY UPDATES
     * ---------------------------------------------------------
     */

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update BatchJob b
        set b.status = 'INGESTING',
            b.lastUpdatedAt = :now
        where b.id = :batchId
    """)
    int markIngestionStarted(
            @Param("batchId") UUID batchId,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update BatchJob b
        set b.status = 'FAILED',
            b.lastUpdatedAt = :now
        where b.id = :batchId
    """)
    int markBatchFailed(
            @Param("batchId") UUID batchId,
            @Param("now") LocalDateTime now
    );

    /*
     * ---------------------------------------------------------
     * DETERMINISTIC / INGESTION COUNTERS
     * ---------------------------------------------------------
     *
     * One atomic UPDATE per chunk.
     * No worker loads counters into Java and writes them back.
     */

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update BatchJob b
        set b.totalLogs = b.totalLogs + :totalDelta,
            b.acceptedLogs = b.acceptedLogs + :acceptedDelta,
            b.deterministicProcessed =
                b.deterministicProcessed + :deterministicDelta,
            b.normalizedLogs =
                b.normalizedLogs + :normalizedDelta,
            b.failedLogs =
                b.failedLogs + :failedDelta,
            b.lastUpdatedAt = :now
        where b.id = :batchId
    """)
    int addChunkCounters(
            @Param("batchId") UUID batchId,
            @Param("totalDelta") long totalDelta,
            @Param("acceptedDelta") long acceptedDelta,
            @Param("deterministicDelta") long deterministicDelta,
            @Param("normalizedDelta") long normalizedDelta,
            @Param("failedDelta") long failedDelta,
            @Param("now") LocalDateTime now
    );

    /*
     * ---------------------------------------------------------
     * AI COUNTERS
     * ---------------------------------------------------------
     */

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update BatchJob b
        set b.aiQueued = b.aiQueued + 1,
            b.status = 'AI_DRAINING',
            b.lastUpdatedAt = :now
        where b.id = :batchId
    """)
    int incrementAiQueued(
            @Param("batchId") UUID batchId,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update BatchJob b
        set b.aiQueued =
                case
                    when b.aiQueued > 0
                    then b.aiQueued - 1
                    else 0
                end,
            b.aiProcessing = b.aiProcessing + 1,
            b.status = 'AI_DRAINING',
            b.lastUpdatedAt = :now
        where b.id = :batchId
    """)
    int markAiStarted(
            @Param("batchId") UUID batchId,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update BatchJob b
        set b.aiProcessing =
                case
                    when b.aiProcessing > 0
                    then b.aiProcessing - 1
                    else 0
                end,
            b.normalizedLogs = b.normalizedLogs + 1,
            b.lastUpdatedAt = :now
        where b.id = :batchId
    """)
    int markAiCompleted(
            @Param("batchId") UUID batchId,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update BatchJob b
        set b.aiProcessing =
                case
                    when b.aiProcessing > 0
                    then b.aiProcessing - 1
                    else 0
                end,
            b.failedLogs = b.failedLogs + 1,
            b.lastUpdatedAt = :now
        where b.id = :batchId
    """)
    int markAiFailed(
            @Param("batchId") UUID batchId,
            @Param("now") LocalDateTime now
    );

    /*
     * Used when the counter was incremented before Kafka publish,
     * but the publish itself failed.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update BatchJob b
        set b.aiQueued =
                case
                    when b.aiQueued > 0
                    then b.aiQueued - 1
                    else 0
                end,
            b.failedLogs = b.failedLogs + 1,
            b.lastUpdatedAt = :now
        where b.id = :batchId
    """)
    int markAiQueueFailed(
            @Param("batchId") UUID batchId,
            @Param("now") LocalDateTime now
    );

    /*
     * ---------------------------------------------------------
     * FINAL STATUS RECONCILIATION
     * ---------------------------------------------------------
     *
     * PostgreSQL computes status from the counters in the same row.
     * No stale Java BatchJob object participates.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(
        value = """
            UPDATE batch_jobs
            SET
                status =
                    CASE
                        WHEN total_logs > 0
                             AND ai_queued = 0
                             AND ai_processing = 0
                             AND (normalized_logs + failed_logs) >= total_logs
                        THEN
                            CASE
                                WHEN failed_logs > 0
                                THEN 'COMPLETED_WITH_ERRORS'
                                ELSE 'COMPLETED'
                            END

                        WHEN ai_queued > 0
                             OR ai_processing > 0
                        THEN 'AI_DRAINING'

                        ELSE 'PROCESSING'
                    END,

                completed_at =
                    CASE
                        WHEN total_logs > 0
                             AND ai_queued = 0
                             AND ai_processing = 0
                             AND (normalized_logs + failed_logs) >= total_logs
                        THEN COALESCE(completed_at, :now)
                        ELSE completed_at
                    END,

                last_updated_at = :now

            WHERE id = :batchId
        """,
        nativeQuery = true
    )
    int refreshStatus(
            @Param("batchId") UUID batchId,
            @Param("now") LocalDateTime now
    );
}
