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

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    public RawLogAiResultService(
            NormalizedLogRepository normalizedLogRepository,
            RawLogRepository rawLogRepository,
            NormalizationEngine normalizationEngine,
            AiSuspicionService aiSuspicionService
    ) {
        this.normalizedLogRepository = normalizedLogRepository;
        this.rawLogRepository = rawLogRepository;
        this.normalizationEngine = normalizationEngine;
        this.aiSuspicionService = aiSuspicionService;
    }

    public void saveAiResult(
            RawLog rawLog,
            ParsedLog parsedLog,
            NormalizationValidationService.ValidationStatus validationStatus,
            String parserUsed
    ) throws Exception {

        UniversalEvent universalEvent =
                normalizationEngine.normalize(
                        rawLog,
                        parsedLog,
                        parserUsed
                );

        AiReviewMetadata aiReview =
                aiSuspicionService.review(
                        rawLog.getRawContent(),
                        parsedLog.fields()
                );

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

        rawLog.setProcessingStatus(
                "NORMALIZED"
        );

        rawLogRepository.save(
                rawLog
        );
    }
}