package com.ntro.ulpf.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ntro.ulpf.entity.ParserLearningCandidate;

public interface ParserLearningCandidateRepository
        extends JpaRepository<
                ParserLearningCandidate,
                UUID
        > {

    Optional<ParserLearningCandidate>
    findByFingerprint(
            String fingerprint
    );

    List<ParserLearningCandidate>
    findByStatusOrderByConfidenceDesc(
            String status
    );

    List<ParserLearningCandidate>
    findAllByOrderByLastSeenAtDesc();
}