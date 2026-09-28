package com.ntro.ulpf.repository;

import com.ntro.ulpf.entity.RawLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RawLogRepository
        extends JpaRepository<RawLog, UUID> {

    List<RawLog>
    findAllByOrderByReceivedAtDesc();

    List<RawLog>
    findByProcessingStatusOrderByReceivedAtDesc(
            String processingStatus
    );

    Page<RawLog>
    findByBatchIdOrderByReceivedAtAsc(
            UUID batchId,
            Pageable pageable
    );
}