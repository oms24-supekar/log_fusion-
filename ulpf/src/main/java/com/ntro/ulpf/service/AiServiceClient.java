package com.ntro.ulpf.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ntro.ulpf.dto.AiMappingResponse;
import com.ntro.ulpf.dto.AiMappingSuggestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceClient {

    private final String aiServiceUrl;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public AiServiceClient(
            @Value("${ai.service.url}")
            String aiServiceUrl
    ) {

        this.aiServiceUrl =
                aiServiceUrl;

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(5)
                        )
                        .build();

        this.objectMapper =
                new ObjectMapper()
                        .findAndRegisterModules();
    }

    public List<AiMappingSuggestion> suggestMappings(
            String rawLog,
            Map<String, Object> extractedFields
    ) {

        try {

            /*
             * Build the list of fields that will
             * be sent to the Python AI service.
             */
            List<Map<String, Object>> candidates =
                    new ArrayList<>();

            for (Map.Entry<String, Object> entry
                    : extractedFields.entrySet()) {

                String fieldName =
                        entry.getKey();

                /*
                 * Ignore token0, token2, token3...
                 *
                 * token1 is kept because it often
                 * represents an action/event.
                 */
                if (fieldName.startsWith("token")
                        && !fieldName.equals("token1")) {

                    continue;
                }

                Map<String, Object> candidate =
                        new LinkedHashMap<>();

                candidate.put(
                        "field_name",
                        fieldName
                );

                candidate.put(
                        "sample_value",
                        entry.getValue() == null
                                ? ""
                                : String.valueOf(
                                        entry.getValue()
                                )
                );

                candidates.add(candidate);
            }

            /*
             * Build the exact FastAPI payload.
             */
            Map<String, Object> payload =
                    new LinkedHashMap<>();

            payload.put(
                    "raw_log",
                    rawLog
            );

            payload.put(
                    "extracted_fields",
                    candidates
            );

            /*
             * Convert the Java Map into REAL JSON.
             */
            String jsonBody =
                    objectMapper
                            .writeValueAsString(
                                    payload
                            );

            System.out.println(
                    "AI REQUEST JSON: "
                            + jsonBody
            );

            /*
             * Send the JSON explicitly.
             */
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            aiServiceUrl
                                                    + "/ai/suggest-mapping"
                                    )
                            )
                            .timeout(
                                    Duration.ofSeconds(10)
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(jsonBody)
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );

            System.out.println(
                    "AI STATUS CODE: "
                            + response.statusCode()
            );

            System.out.println(
                    "AI RESPONSE JSON: "
                            + response.body()
            );

            /*
             * Anything outside 2xx means
             * the AI request failed.
             */
            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "AI service returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            AiMappingResponse aiResponse =
                    objectMapper.readValue(
                            response.body(),
                            AiMappingResponse.class
                    );

            if (aiResponse == null
                    || aiResponse.suggestions()
                    == null) {

                return List.of();
            }

            return aiResponse.suggestions();

        } catch (Exception e) {

            System.err.println(
                    "AI SERVICE ERROR: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Failed to call AI service",
                    e
            );
        }
    }
}