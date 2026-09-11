package com.ntro.ulpf;

import com.ntro.ulpf.entity.ParserCreationMode;
import com.ntro.ulpf.entity.ParserDefinition;
import com.ntro.ulpf.repository.ParserDefinitionRepository;
import com.ntro.ulpf.service.NormalizationConfidenceService;
import com.ntro.ulpf.service.ParserLearningService;
import com.ntro.ulpf.service.ParserMatchingService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AdaptiveNormalizerServiceTest {

    @Test
    void autoApprovalThresholdIsConfigurable() {
        NormalizationConfidenceService service =
                new NormalizationConfidenceService(
                        true,
                        0.95,
                        true
                );

        assertTrue(service.shouldAutoApprove(0.96));
        assertFalse(service.shouldAutoApprove(0.90));
        assertTrue(service.getAutoApproveThreshold() > 0.0);
    }

    @Test
    void parserMatchingAndLearningUseReliableSignals() {
        ParserDefinition definition = new ParserDefinition(
                UUID.randomUUID(),
                "adaptive-json",
                "__JSON__",
                ":",
                ":",
                "{\"timestamp\":\"value\",\"source_ip\":\"value\"}",
                true,
                0.92,
                ParserCreationMode.AUTO,
                9,
                1,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ParserMatchingService matchingService = new ParserMatchingService();
        String rawLog = "{\"timestamp\":\"2026-09-11T12:00:00Z\",\"source_ip\":\"10.0.0.9\",\"severity\":\"INFO\"}";

        assertTrue(matchingService.matches(definition, rawLog));
        assertTrue(matchingService.scoreMatch(definition, rawLog) >= 0.90);

        ParserDefinitionRepository repository = mock(ParserDefinitionRepository.class);
        ParserLearningService learningService = new ParserLearningService(repository);

        assertEquals(0.90, learningService.calculateReliability(definition), 0.01);
        assertTrue(learningService.isReliable(definition));
    }
}
