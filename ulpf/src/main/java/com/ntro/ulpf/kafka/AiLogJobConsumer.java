package com.ntro.ulpf.kafka;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ntro.ulpf.detection.LogFormat;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.parser.ParsedLog;
import com.ntro.ulpf.repository.RawLogRepository;
import com.ntro.ulpf.service.AiServiceClient;
import com.ntro.ulpf.service.BatchProgressService;
import com.ntro.ulpf.service.NormalizationValidationService;
import com.ntro.ulpf.service.RawLogAiResultService;

@Service
public class AiLogJobConsumer {

    private final AiServiceClient aiServiceClient;

    private final NormalizationValidationService
            validationService;

    private final RawLogRepository rawLogRepository;

    private final RawLogAiResultService
            rawLogAiResultService;

    private final BatchProgressService
            batchProgressService;

    public AiLogJobConsumer(
            AiServiceClient aiServiceClient,
            NormalizationValidationService validationService,
            RawLogRepository rawLogRepository,
            RawLogAiResultService rawLogAiResultService,
            BatchProgressService batchProgressService
    ) {

        this.aiServiceClient =
                aiServiceClient;

        this.validationService =
                validationService;

        this.rawLogRepository =
                rawLogRepository;

        this.rawLogAiResultService =
                rawLogAiResultService;

        this.batchProgressService =
                batchProgressService;
    }

    @KafkaListener(
            topics =
                    "${logfusion.kafka.topics.raw-ai}",
            groupId =
                    "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            AiLogJob job
    ) {

        RawLog rawLog =
                null;

        try {

            rawLog =
                    rawLogRepository
                            .findById(
                                    job.rawLogId()
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Raw log not found: "
                                                            + job.rawLogId()
                                            )
                            );

            /*
             * =================================================
             * AI STARTED
             * =================================================
             */

            batchProgressService
                    .markAiStarted(
                            rawLog.getBatchId()
                    );

            rawLog.setProcessingStatus(
                    "AI_NORMALIZING"
            );

            rawLogRepository.save(
                    rawLog
            );

            /*
             * =================================================
             * OLLAMA NORMALIZATION
             * =================================================
             */

            Map<String, Object> aiFields =
                    aiServiceClient
                            .normalizeLog(
                                    job.rawContent()
                            );

            /*
             * =================================================
             * VALIDATION
             * =================================================
             */

            var validation =
                    validationService
                            .validate(
                                    aiFields
                            );

            ParsedLog parsedLog =
                    new ParsedLog(
                            LogFormat.DYNAMIC,
                            aiFields
                    );

            /*
             * =================================================
             * SAVE NORMALIZED RESULT
             * =================================================
             */

            rawLogAiResultService
                    .saveAiResult(
                            rawLog,
                            parsedLog,
                            validation,
                            "OLLAMA:qwen2.5:3b"
                    );

            /*
             * =================================================
             * UPDATE BATCH COUNTERS
             * =================================================
             */

            batchProgressService
                    .markAiCompleted(
                            rawLog.getBatchId()
                    );

        } catch (Exception e) {

            /*
             * =================================================
             * FAILURE HANDLING
             * =================================================
             */

            if (rawLog != null) {

                rawLog.setProcessingStatus(
                        "AI_FAILED"
                );

                rawLogRepository.save(
                        rawLog
                );

                batchProgressService
                        .markAiFailed(
                                rawLog.getBatchId()
                        );
            }

            System.err.println(
                    "AI Kafka consumer failed for "
                            + job.rawLogId()
                            + ": "
                            + e.getMessage()
            );
        }
    }
}