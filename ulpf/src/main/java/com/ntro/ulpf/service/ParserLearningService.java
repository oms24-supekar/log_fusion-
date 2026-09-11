package com.ntro.ulpf.service;

import com.ntro.ulpf.entity.ParserDefinition;
import com.ntro.ulpf.repository.ParserDefinitionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ParserLearningService {

    private final ParserDefinitionRepository parserDefinitionRepository;

    public ParserLearningService(ParserDefinitionRepository parserDefinitionRepository) {
        this.parserDefinitionRepository = parserDefinitionRepository;
    }

    public void recordSuccess(ParserDefinition definition) {
        if (definition == null) {
            return;
        }

        definition.setSuccessfulMatches(definition.getSuccessfulMatches() + 1);
        definition.setLastMatchedAt(LocalDateTime.now());
        definition.setEnabled(true);
        if (definition.getConfidence() == null) {
            definition.setConfidence(0.0);
        }
        definition.setConfidence(Math.min(1.0, definition.getConfidence() + 0.05));
        parserDefinitionRepository.save(definition);
    }

    public void recordFailure(ParserDefinition definition) {
        if (definition == null) {
            return;
        }

        definition.setFailedMatches(definition.getFailedMatches() + 1);
        definition.setLastMatchedAt(LocalDateTime.now());
        if (definition.getConfidence() == null) {
            definition.setConfidence(0.0);
        }
        definition.setConfidence(Math.max(0.0, definition.getConfidence() - 0.10));
        if (definition.getFailedMatches() >= 3) {
            definition.setEnabled(false);
        }
        parserDefinitionRepository.save(definition);
    }

    public double calculateReliability(ParserDefinition definition) {
        if (definition == null) {
            return 0.0;
        }

        long total = definition.getSuccessfulMatches() + definition.getFailedMatches();
        if (total == 0) {
            return definition.getConfidence() == null ? 0.0 : definition.getConfidence();
        }

        double ratio = (double) definition.getSuccessfulMatches() / (double) total;
        if (definition.getConfidence() != null) {
            return Math.min(1.0, (ratio * 0.7) + (definition.getConfidence() * 0.3));
        }
        return ratio;
    }

    public boolean isReliable(ParserDefinition definition) {
        return calculateReliability(definition) >= 0.80 && !definition.isEnabled() == false;
    }
}
