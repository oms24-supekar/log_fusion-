package com.ntro.ulpf.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AiLogJobPublisher {

    private final KafkaTemplate<String, AiLogJob> kafkaTemplate;
    private final String rawAiTopic;

    public AiLogJobPublisher(
            KafkaTemplate<String, AiLogJob> kafkaTemplate,
            @Value("${logfusion.kafka.topics.raw-ai}") String rawAiTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.rawAiTopic = rawAiTopic;
    }

    public CompletableFuture<SendResult<String, AiLogJob>> publish(
            AiLogJob job
    ) {
        return kafkaTemplate.send(
                rawAiTopic,
                job.rawLogId().toString(),
                job
        );
    }
}