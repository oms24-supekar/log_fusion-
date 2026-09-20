package com.ntro.ulpf.kafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ntro.ulpf.detection.LogFormat;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.parser.ParsedLog;
import com.ntro.ulpf.repository.RawLogRepository;
import com.ntro.ulpf.service.AiServiceClient;
import com.ntro.ulpf.service.NormalizationValidationService;
import com.ntro.ulpf.service.RawLogAiResultService;



import java.util.Map;

@Service
public class AiLogJobConsumer {

    private final AiServiceClient aiServiceClient;
    private final NormalizationValidationService validationService;
    private final RawLogRepository rawLogRepository;
    private final RawLogAiResultService rawLogAiResultService;

    public AiLogJobConsumer(
            AiServiceClient aiServiceClient,
            NormalizationValidationService validationService,
            RawLogRepository rawLogRepository,
            RawLogAiResultService rawLogAiResultService
    ) {
        this.aiServiceClient = aiServiceClient;
        this.validationService = validationService;
        this.rawLogRepository = rawLogRepository;
        this.rawLogAiResultService = rawLogAiResultService;
    }

    @KafkaListener(
            topics = "${logfusion.kafka.topics.raw-ai}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(AiLogJob job) {

        try {

            RawLog rawLog =
                    rawLogRepository
                            .findById(job.rawLogId())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Raw log not found: "
                                                    + job.rawLogId()
                                    )
                            );

            rawLog.setProcessingStatus(
                    "AI_NORMALIZING"
            );

            rawLogRepository.save(rawLog);

            Map<String, Object> aiFields =
                    aiServiceClient
                            .normalizeLog(
                                    job.rawContent()
                            );

            var validation =
                    validationService
                            .validate(aiFields);

            ParsedLog parsedLog =
                    new ParsedLog(
                            LogFormat.DYNAMIC,
                            aiFields
                    );

            rawLogAiResultService
                    .saveAiResult(
                            rawLog,
                            parsedLog,
                            validation,
                            "OLLAMA:qwen2.5:3b"
                    );

        } catch (Exception e) {

            rawLogRepository
                    .findById(job.rawLogId())
                    .ifPresent(rawLog -> {

                        rawLog.setProcessingStatus(
                                "AI_FAILED"
                        );

                        rawLogRepository.save(rawLog);
                    });

            System.err.println(
                    "AI Kafka consumer failed for "
                            + job.rawLogId()
                            + ": "
                            + e.getMessage()
            );
        }
    }
}