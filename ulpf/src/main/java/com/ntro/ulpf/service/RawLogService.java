package com.ntro.ulpf.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntro.ulpf.detection.FormatDetector;
import com.ntro.ulpf.detection.LogFormat;
import com.ntro.ulpf.dto.LogRequest;
import com.ntro.ulpf.dto.LogResponse;
import com.ntro.ulpf.entity.NormalizedLog;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.normalization.NormalizationEngine;
import com.ntro.ulpf.normalization.UniversalEvent;
import com.ntro.ulpf.parser.DynamicParseResult;
import com.ntro.ulpf.parser.LogParser;
import com.ntro.ulpf.parser.ParsedLog;
import com.ntro.ulpf.parser.ParserRegistry;
import com.ntro.ulpf.repository.NormalizedLogRepository;
import com.ntro.ulpf.repository.RawLogRepository;
import com.ntro.ulpf.util.HashUtil;

@Service
public class RawLogService {

    private final RawLogRepository rawLogRepository;

    private final NormalizedLogRepository
            normalizedLogRepository;

    private final FormatDetector
            formatDetector;

    private final ParserRegistry
            parserRegistry;

    private final NormalizationEngine
            normalizationEngine;

    private final DynamicParserService
            dynamicParserService;

    private final AutomaticNormalizationService
            automaticNormalizationService;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    public RawLogService(
            RawLogRepository rawLogRepository,
            NormalizedLogRepository normalizedLogRepository,
            FormatDetector formatDetector,
            ParserRegistry parserRegistry,
            NormalizationEngine normalizationEngine,
            DynamicParserService dynamicParserService,
            AutomaticNormalizationService automaticNormalizationService
    ) {

        this.rawLogRepository =
                rawLogRepository;

        this.normalizedLogRepository =
                normalizedLogRepository;

        this.formatDetector =
                formatDetector;

        this.parserRegistry =
                parserRegistry;

        this.normalizationEngine =
                normalizationEngine;

        this.dynamicParserService =
                dynamicParserService;

        this.automaticNormalizationService =
                automaticNormalizationService;
    }

    /*
     * =========================================================
     * NEW LOG INGESTION
     * =========================================================
     */
    public LogResponse saveRawLog(
            LogRequest request
    ) {

        UUID id =
                UUID.randomUUID();

        String hash =
                HashUtil.sha256(
                        request.rawContent()
                );

        LocalDateTime receivedAt =
                request.receivedAt() != null
                        ? request.receivedAt()
                        : LocalDateTime.now();

        /*
         * =====================================================
         * FORMAT DETECTION
         * =====================================================
         */
        LogFormat detectedFormat =
                formatDetector.detect(
                        request.rawContent()
                );

        String initialStatus =
                detectedFormat
                        == LogFormat.UNKNOWN
                        ? "NEEDS_REVIEW"
                        : "DETECTED";

        /*
         * =====================================================
         * PRESERVE RAW FORENSIC EVIDENCE
         * =====================================================
         */
        RawLog rawLog =
                new RawLog(
                        id,
                        request.rawContent(),
                        request.sourceName(),
                        request.sourceType(),
                        detectedFormat.name(),
                        hash,
                        receivedAt,
                        initialStatus
                );

        /*
         * Batch traceability.
         *
         * Normal single-log requests simply have batchId = null.
         */
        rawLog.setBatchId(
                request.batchId()
        );

        RawLog savedLog =
                rawLogRepository.save(
                        rawLog
                );

        /*
         * =====================================================
         * KNOWN FORMAT
         * =====================================================
         *
         * Fast path:
         *
         * JSON
         * SYSLOG
         * CEF
         * KEY_VALUE
         * etc.
         *
         * If the deterministic parser fails,
         * DO NOT immediately mark FAILED.
         *
         * Route to AI instead.
         */
        if (detectedFormat
                != LogFormat.UNKNOWN) {

            try {

                LogParser parser =
                        parserRegistry.getParser(
                                detectedFormat
                        );

                ParsedLog parsedLog =
                        parser.parse(
                                request.rawContent()
                        );

                return processParsedLog(
                        savedLog,
                        parsedLog,
                        parser.getClass()
                                .getSimpleName()
                );

            } catch (Exception parserException) {

                System.out.println(
                        "Deterministic parser failed for "
                                + savedLog.getId()
                                + " ["
                                + detectedFormat
                                + "]"
                                + " → routing to AI. Reason: "
                                + parserException.getMessage()
                );

                return routeToAi(
                        savedLog
                );
            }
        }

        /*
         * =====================================================
         * UNKNOWN FORMAT
         * =====================================================
         *
         * First try any previously learned / approved
         * dynamic parser.
         */
        try {

            Optional<DynamicParseResult>
                    dynamicResult =
                    dynamicParserService
                            .tryParse(
                                    request.rawContent()
                            );

            if (dynamicResult.isPresent()) {

                DynamicParseResult result =
                        dynamicResult.get();

                savedLog.setDetectedFormat(
                        LogFormat.DYNAMIC.name()
                );

                savedLog.setProcessingStatus(
                        "DETECTED"
                );

                savedLog =
                        rawLogRepository.save(
                                savedLog
                        );

                String parserUsed =
                        "Dynamic:"
                                + result
                                .definition()
                                .getName();

                try {

                    return processParsedLog(
                            savedLog,
                            result.parsedLog(),
                            parserUsed
                    );

                } catch (Exception dynamicFailure) {

                    System.out.println(
                            "Dynamic parser failed for "
                                    + savedLog.getId()
                                    + " → routing to AI."
                    );

                    return routeToAi(
                            savedLog
                    );
                }
            }

            /*
             * No dynamic parser matched.
             *
             * AutomaticNormalizationService can:
             *
             * 1. inspect deterministic structure
             * 2. auto-approve if confident
             * 3. otherwise queue to Kafka / AI
             */
            return automaticNormalizationService
                    .process(
                            savedLog
                    );

        } catch (Exception e) {

            /*
             * Even dynamic analysis itself failed.
             *
             * AI becomes the final normalization fallback.
             */
            System.out.println(
                    "Unknown-log analysis failed for "
                            + savedLog.getId()
                            + " → routing directly to AI. Reason: "
                            + e.getMessage()
            );

            return routeToAi(
                    savedLog
            );
        }
    }

    /*
     * =========================================================
     * UNIVERSAL AI FALLBACK
     * =========================================================
     *
     * Any deterministic parser failure can land here.
     *
     * Known format failure
     * Dynamic parser failure
     * Unknown structure
     * Weird vendor variation
     * Malformed but understandable telemetry
     *
     * → Kafka
     * → Ollama/Qwen
     */
    private LogResponse routeToAi(
            RawLog rawLog
    ) {

        try {

            return automaticNormalizationService
                    .queueDirectlyForAi(
                            rawLog
                    );

        } catch (Exception e) {

            /*
             * Only mark FAILED if even AI queueing
             * itself cannot be started.
             */
            return markFailed(
                    rawLog,
                    e
            );
        }
    }

    /*
     * =========================================================
     * REPROCESS EXISTING LOG
     * =========================================================
     *
     * Used after a reusable dynamic parser has been learned
     * or approved.
     */
    public LogResponse reprocessDynamicLog(
            UUID rawLogId
    ) {

        RawLog savedLog =
                rawLogRepository
                        .findById(
                                rawLogId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Raw log not found: "
                                                        + rawLogId
                                        )
                        );

        /*
         * Never normalize the same raw log twice.
         */
        if (normalizedLogRepository
                .findByRawLog_Id(
                        rawLogId
                )
                .isPresent()) {

            return toResponse(
                    savedLog
            );
        }

        try {

            Optional<DynamicParseResult>
                    dynamicResult =
                    dynamicParserService
                            .tryParse(
                                    savedLog
                                            .getRawContent()
                            );

            /*
             * No learned parser matches.
             *
             * Do not leave it stuck in NEEDS_REVIEW.
             * Let AI attempt normalization.
             */
            if (dynamicResult.isEmpty()) {

                savedLog.setDetectedFormat(
                        LogFormat.UNKNOWN.name()
                );

                savedLog =
                        rawLogRepository.save(
                                savedLog
                        );

                return routeToAi(
                        savedLog
                );
            }

            DynamicParseResult result =
                    dynamicResult.get();

            savedLog.setDetectedFormat(
                    LogFormat.DYNAMIC.name()
            );

            savedLog.setProcessingStatus(
                    "DETECTED"
            );

            savedLog =
                    rawLogRepository.save(
                            savedLog
                    );

            String parserUsed =
                    "Dynamic:"
                            + result
                            .definition()
                            .getName();

            try {

                return processParsedLog(
                        savedLog,
                        result.parsedLog(),
                        parserUsed
                );

            } catch (Exception parserFailure) {

                return routeToAi(
                        savedLog
                );
            }

        } catch (Exception e) {

            return routeToAi(
                    savedLog
            );
        }
    }

    /*
     * =========================================================
     * NORMALIZATION + STORAGE
     * =========================================================
     */
    private LogResponse processParsedLog(
            RawLog savedLog,
            ParsedLog parsedLog,
            String parserUsed
    ) throws Exception {

        UniversalEvent universalEvent =
                normalizationEngine
                        .normalize(
                                savedLog,
                                parsedLog,
                                parserUsed
                        );

        String normalizedJson =
                objectMapper
                        .writeValueAsString(
                                universalEvent
                        );

        NormalizedLog normalizedLog =
                new NormalizedLog(
                        universalEvent.eventId(),
                        savedLog,
                        normalizedJson,
                        parserUsed,
                        universalEvent
                                .metadata()
                                .processedAt(),
                        "VALID"
                );

        normalizedLogRepository.save(
                normalizedLog
        );

        savedLog.setProcessingStatus(
                "NORMALIZED"
        );

        savedLog =
                rawLogRepository.save(
                        savedLog
                );

        return toResponse(
                savedLog
        );
    }

    /*
     * =========================================================
     * FINAL FAILURE
     * =========================================================
     *
     * We reach this only if the normal pipeline AND
     * AI fallback cannot proceed.
     */
    private LogResponse markFailed(
            RawLog savedLog,
            Exception exception
    ) {

        savedLog.setProcessingStatus(
                "FAILED"
        );

        savedLog =
                rawLogRepository.save(
                        savedLog
                );

        System.err.println(
                "Log processing permanently failed for "
                        + savedLog.getId()
                        + ": "
                        + exception.getMessage()
        );

        return toResponse(
                savedLog
        );
    }

    /*
     * =========================================================
     * RESPONSE
     * =========================================================
     */
    private LogResponse toResponse(
            RawLog savedLog
    ) {

        return new LogResponse(
                savedLog.getId(),
                savedLog.getSourceName(),
                savedLog.getSourceType(),
                savedLog.getDetectedFormat(),
                savedLog.getSha256Hash(),
                savedLog.getProcessingStatus(),
                savedLog.getReceivedAt()
        );
    }
}