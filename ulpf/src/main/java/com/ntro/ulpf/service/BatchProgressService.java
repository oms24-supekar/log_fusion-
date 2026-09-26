package com.ntro.ulpf.service;

import com.ntro.ulpf.repository.BatchJobRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BatchProgressService {

    private final BatchJobRepository batchJobRepository;

    public BatchProgressService(
            BatchJobRepository batchJobRepository
    ) {
        this.batchJobRepository =
                batchJobRepository;
    }

    /*
     * IMPORTANT:
     *
     * Call this BEFORE publishing the Kafka job.
     * That prevents the consumer from starting before
     * aiQueued has been incremented.
     */
    public void markAiQueued(
            UUID batchId
    ) {
        if (batchId == null) {
            return;
        }

        batchJobRepository.incrementAiQueued(
                batchId,
                LocalDateTime.now()
        );
    }

    public void markAiStarted(
            UUID batchId
    ) {
        if (batchId == null) {
            return;
        }

        batchJobRepository.markAiStarted(
                batchId,
                LocalDateTime.now()
        );
    }

    public void markAiCompleted(
            UUID batchId
    ) {
        if (batchId == null) {
            return;
        }

        LocalDateTime now =
                LocalDateTime.now();

        batchJobRepository.markAiCompleted(
                batchId,
                now
        );

        batchJobRepository.refreshStatus(
                batchId,
                now
        );
    }

    public void markAiFailed(
            UUID batchId
    ) {
        if (batchId == null) {
            return;
        }

        LocalDateTime now =
                LocalDateTime.now();

        batchJobRepository.markAiFailed(
                batchId,
                now
        );

        batchJobRepository.refreshStatus(
                batchId,
                now
        );
    }

    public void markAiQueueFailed(
            UUID batchId
    ) {
        if (batchId == null) {
            return;
        }

        LocalDateTime now =
                LocalDateTime.now();

        batchJobRepository.markAiQueueFailed(
                batchId,
                now
        );

        batchJobRepository.refreshStatus(
                batchId,
                now
        );
    }

    public void refreshBatchStatus(
            UUID batchId
    ) {
        if (batchId == null) {
            return;
        }

        batchJobRepository.refreshStatus(
                batchId,
                LocalDateTime.now()
        );
    }
}
