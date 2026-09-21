package com.ntro.ulpf.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ntro.ulpf.entity.BatchJob;

public interface BatchJobRepository
        extends JpaRepository<BatchJob, UUID> {

    List<BatchJob>
    findAllByOrderByStartedAtDesc();

    List<BatchJob>
    findByStatusOrderByStartedAtDesc(
            String status
    );

    @Modifying
    @Transactional
    @Query("""
        update BatchJob b
        set
            b.aiQueued = b.aiQueued + 1,
            b.lastUpdatedAt = :now
        where b.id = :batchId
    """)
    int incrementAiQueued(
            @Param("batchId")
            UUID batchId,

            @Param("now")
            LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("""
        update BatchJob b
        set
            b.aiQueued =
                case
                    when b.aiQueued > 0
                    then b.aiQueued - 1
                    else 0
                end,

            b.aiProcessing =
                b.aiProcessing + 1,

            b.status =
                'AI_DRAINING',

            b.lastUpdatedAt =
                :now

        where b.id = :batchId
    """)
    int markAiStarted(
            @Param("batchId")
            UUID batchId,

            @Param("now")
            LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("""
        update BatchJob b
        set
            b.aiProcessing =
                case
                    when b.aiProcessing > 0
                    then b.aiProcessing - 1
                    else 0
                end,

            b.normalizedLogs =
                b.normalizedLogs + 1,

            b.lastUpdatedAt =
                :now

        where b.id = :batchId
    """)
    int markAiCompleted(
            @Param("batchId")
            UUID batchId,

            @Param("now")
            LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("""
        update BatchJob b
        set
            b.aiProcessing =
                case
                    when b.aiProcessing > 0
                    then b.aiProcessing - 1
                    else 0
                end,

            b.failedLogs =
                b.failedLogs + 1,

            b.lastUpdatedAt =
                :now

        where b.id = :batchId
    """)
    int markAiFailed(
            @Param("batchId")
            UUID batchId,

            @Param("now")
            LocalDateTime now
    );
}
