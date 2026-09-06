package com.ntro.ulpf.repository;

import com.ntro.ulpf.entity.NormalizedLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NormalizedLogRepository
        extends JpaRepository<NormalizedLog, UUID> {

    Optional<NormalizedLog> findByRawLog_Id(UUID rawLogId);
}