package com.ntro.ulpf.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ntro.ulpf.entity.BatchJob;
import com.ntro.ulpf.repository.BatchJobRepository;

@Service
public class BatchProgressService {

    private final BatchJobRepository batchJobRepository;

    public BatchProgressService(
            BatchJobRepository batchJobRepository
    ) {
        this.batchJobRepository =
                batchJobRepository;
    }

    public void markAiQueued(
            UUID batchId
    ) {

        if (batchId == null) {
            return;
        }

        batchJobRepository
                .incrementAiQueued(
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

        batchJobRepository
                .markAiStarted(
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

        batchJobRepository
                .markAiCompleted(
                        batchId,
                        LocalDateTime.now()
                );

        finalizeBatch(
                batchId
        );
    }

    public void markAiFailed(
            UUID batchId
    ) {

        if (batchId == null) {
            return;
        }

        batchJobRepository
                .markAiFailed(
                        batchId,
                        LocalDateTime.now()
                );

        finalizeBatch(
                batchId
        );
    }

    public void finalizeBatch(
            UUID batchId
    ) {

        BatchJob batch =
                batchJobRepository
                        .findById(
                                batchId
                        )
                        .orElse(null);

        if (batch == null) {
            return;
        }

        boolean aiFinished =
                batch.getAiQueued() == 0
                        &&
                batch.getAiProcessing() == 0;

        long handled =
                batch.getNormalizedLogs()
                        +
                batch.getFailedLogs();

        boolean allHandled =
                batch.getTotalLogs() > 0
                        &&
                handled >= batch.getTotalLogs();

        if (
                aiFinished
                        &&
                allHandled
        ) {

            if (batch.getFailedLogs() > 0) {

                batch.setStatus(
                        "COMPLETED_WITH_ERRORS"
                );

            } else {

                batch.setStatus(
                        "COMPLETED"
                );
            }

            batch.setCompletedAt(
                    LocalDateTime.now()
            );

        } else {

            batch.setStatus(
                    "AI_DRAINING"
            );
        }

        batch.setLastUpdatedAt(
                LocalDateTime.now()
        );

        batchJobRepository.save(
                batch
        );
    }
}