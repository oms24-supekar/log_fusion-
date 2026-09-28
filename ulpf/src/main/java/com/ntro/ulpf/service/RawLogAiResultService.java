package com.ntro.ulpf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntro.ulpf.dto.AiReviewMetadata;
import com.ntro.ulpf.entity.NormalizedLog;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.normalization.NormalizationEngine;
import com.ntro.ulpf.normalization.UniversalEvent;
import com.ntro.ulpf.parser.ParsedLog;
import com.ntro.ulpf.repository.NormalizedLogRepository;
import com.ntro.ulpf.repository.RawLogRepository;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RawLogAiResultService {

    private final NormalizedLogRepository normalizedLogRepository;
    private final RawLogRepository rawLogRepository;
    private final NormalizationEngine normalizationEngine;
    private final AiSuspicionService aiSuspicionService;
    private final ParserLearningService parserLearningService;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    public RawLogAiResultService(
            NormalizedLogRepository normalizedLogRepository,
            RawLogRepository rawLogRepository,
            NormalizationEngine normalizationEngine,
            AiSuspicionService aiSuspicionService,
            ParserLearningService parserLearningService
    ) {
        this.normalizedLogRepository =
                normalizedLogRepository;

        this.rawLogRepository =
                rawLogRepository;

        this.normalizationEngine =
                normalizationEngine;

        this.aiSuspicionService =
                aiSuspicionService;

        this.parserLearningService =
                parserLearningService;
    }

    public void saveAiResult(
            RawLog rawLog,
            ParsedLog parsedLog,
            NormalizationValidationService.ValidationStatus validationStatus,
            String parserUsed
    ) throws Exception {

        /*
         * =====================================================
         * NORMALIZE AI OUTPUT
         * =====================================================
         */
        UniversalEvent universalEvent =
                normalizationEngine.normalize(
                        rawLog,
                        parsedLog,
                        parserUsed
                );

        /*
         * =====================================================
         * FIELD-LEVEL AI REVIEW
         * =====================================================
         */
        AiReviewMetadata aiReview =
                aiSuspicionService.review(
                        rawLog.getRawContent(),
                        parsedLog.fields()
                );

        /*
         * =====================================================
         * STORED RESULT
         * =====================================================
         *
         * {
         *   normalized: {...},
         *   aiReview: {...}
         * }
         */
        Map<String, Object> storedResult =
                new LinkedHashMap<>();

        storedResult.put(
                "normalized",
                universalEvent
        );

        storedResult.put(
                "aiReview",
                aiReview
        );

        String normalizedJson =
                objectMapper.writeValueAsString(
                        storedResult
                );

        /*
         * =====================================================
         * SAVE NORMALIZED LOG
         * =====================================================
         */
        NormalizedLog normalizedLog =
                new NormalizedLog(
                        UUID.randomUUID(),
                        rawLog,
                        normalizedJson,
                        parserUsed,
                        universalEvent
                                .metadata()
                                .processedAt(),
                        validationStatus.name()
                );

        normalizedLogRepository.save(
                normalizedLog
        );

        /*
         * =====================================================
         * LEARNING LAYER
         * =====================================================
         *
         * Every successful AI normalization now feeds
         * the parser learning system.
         *
         * The same structural family will accumulate
         * examples over time.
         */
        parserLearningService
                .learnFromSuccessfulAiNormalization(
                        rawLog.getRawContent(),
                        parsedLog
                );

        /*
         * =====================================================
         * FINAL RAW LOG STATUS
         * =====================================================
         */
        rawLog.setProcessingStatus(
                "NORMALIZED"
        );

        rawLogRepository.save(
                rawLog
        );
    }
}