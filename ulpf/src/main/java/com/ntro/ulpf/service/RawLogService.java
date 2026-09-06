package com.ntro.ulpf.service;

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

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RawLogService {

    private final RawLogRepository rawLogRepository;

    private final NormalizedLogRepository
            normalizedLogRepository;

    private final FormatDetector formatDetector;

    private final ParserRegistry parserRegistry;

    private final NormalizationEngine
            normalizationEngine;

    private final DynamicParserService
            dynamicParserService;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    public RawLogService(
            RawLogRepository rawLogRepository,
            NormalizedLogRepository normalizedLogRepository,
            FormatDetector formatDetector,
            ParserRegistry parserRegistry,
            NormalizationEngine normalizationEngine,
            DynamicParserService dynamicParserService
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
                LocalDateTime.now();

        LogFormat detectedFormat =
                formatDetector.detect(
                        request.rawContent()
                );

        String initialStatus =
                detectedFormat
                        == LogFormat.UNKNOWN
                        ? "NEEDS_REVIEW"
                        : "DETECTED";

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
         * Preserve forensic evidence FIRST.
         */
        RawLog savedLog =
                rawLogRepository.save(rawLog);

        /*
         * Known deterministic format.
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

            } catch (Exception e) {

                return markFailed(
                        savedLog,
                        e
                );
            }
        }

        /*
         * UNKNOWN FORMAT
         *
         * Check whether a previously approved
         * dynamic parser can understand it.
         */
        try {

            Optional<DynamicParseResult>
                    dynamicResult =
                    dynamicParserService
                            .tryParse(
                                    request.rawContent()
                            );

            /*
             * No dynamic parser exists yet.
             *
             * Keep the raw evidence and send
             * the log to the review/AI workflow.
             */
            if (dynamicResult.isEmpty()) {

                return toResponse(savedLog);
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
                            + result.definition()
                                    .getName();

            return processParsedLog(
                    savedLog,
                    result.parsedLog(),
                    parserUsed
            );

        } catch (Exception e) {

            return markFailed(
                    savedLog,
                    e
            );
        }
    }

    /*
     * =========================================================
     * REPROCESS AN EXISTING UNKNOWN LOG
     * =========================================================
     *
     * Used after a human approves an AI/deterministic mapping
     * and a new dynamic parser definition has been created.
     *
     * IMPORTANT:
     * We DO NOT create another RawLog.
     *
     * The same forensic raw-log UUID is reused.
     */
    public LogResponse reprocessDynamicLog(
            UUID rawLogId
    ) {

        RawLog savedLog =
                rawLogRepository
                        .findById(rawLogId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Raw log not found: "
                                                + rawLogId
                                )
                        );

        /*
         * Safety check:
         * don't normalize the same raw log twice.
         */
        if (normalizedLogRepository
                .findByRawLog_Id(rawLogId)
                .isPresent()) {

            return toResponse(savedLog);
        }

        try {

            Optional<DynamicParseResult>
                    dynamicResult =
                    dynamicParserService
                            .tryParse(
                                    savedLog.getRawContent()
                            );

            /*
             * Still no matching parser.
             *
             * Keep it in the review queue.
             */
            if (dynamicResult.isEmpty()) {

                savedLog.setDetectedFormat(
                        LogFormat.UNKNOWN.name()
                );

                savedLog.setProcessingStatus(
                        "NEEDS_REVIEW"
                );

                savedLog =
                        rawLogRepository.save(
                                savedLog
                        );

                return toResponse(savedLog);
            }

            /*
             * Matching dynamic parser found.
             */
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
                            + result.definition()
                                    .getName();

            /*
             * Normalize the ORIGINAL stored raw log.
             */
            return processParsedLog(
                    savedLog,
                    result.parsedLog(),
                    parserUsed
            );

        } catch (Exception e) {

            return markFailed(
                    savedLog,
                    e
            );
        }
    }

    /*
     * =========================================================
     * NORMALIZATION + NORMALIZED STORAGE
     * =========================================================
     */
    private LogResponse processParsedLog(
            RawLog savedLog,
            ParsedLog parsedLog,
            String parserUsed
    ) {

        try {

            UniversalEvent universalEvent =
                    normalizationEngine.normalize(
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

            return toResponse(savedLog);

        } catch (Exception e) {

            return markFailed(
                    savedLog,
                    e
            );
        }
    }

    /*
     * =========================================================
     * FAILURE HANDLING
     * =========================================================
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
                "Log processing failed for "
                        + savedLog.getId()
                        + ": "
                        + exception.getMessage()
        );

        return toResponse(savedLog);
    }

    /*
     * =========================================================
     * RESPONSE MAPPER
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