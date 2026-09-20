package com.ntro.ulpf.repository;

import com.ntro.ulpf.entity.BatchJob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BatchJobRepository
        extends JpaRepository<
                BatchJob,
                UUID
        > {

    List<BatchJob>
    findAllByOrderByStartedAtDesc();

    List<BatchJob>
    findByStatusOrderByStartedAtDesc(
            String status
    );
}