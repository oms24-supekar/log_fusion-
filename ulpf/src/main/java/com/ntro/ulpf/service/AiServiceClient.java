package com.ntro.ulpf.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ntro.ulpf.dto.AiMappingSuggestion;

import java.util.ArrayList;
import java.util.List;
@Service
public class AiServiceClient {

    private final String ollamaUrl;
    private final String model;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiServiceClient(
            @Value("${ollama.url:http://localhost:11434}")
            String ollamaUrl,

            @Value("${ollama.model:qwen2.5:3b}")
            String model
    ) {

        this.ollamaUrl = ollamaUrl;
        this.model = model;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules();
    }

    public Map<String, Object> normalizeLog(String rawLog) {

        try {

            String prompt = """
                    You are a cybersecurity log normalization engine.

                    Convert the supplied raw log into structured JSON.

                    IMPORTANT RULES:

                    1. Return ONLY JSON.
                    2. Do not use markdown.
                    3. Never invent values.
                    4. If a value does not exist in the log, use null.
                    5. Do not infer destination IP, destination port,
                       severity, username, protocol, or other fields
                       unless directly supported by the log.
                    6. Keep IP addresses exactly as they appear.
                    7. Ports must be numbers or null.

                    Return exactly these fields:

                    timestamp
                    host
                    source_type
                    service
                    action
                    severity
                    username
                    source_ip
                    source_port
                    destination_ip
                    destination_port
                    protocol
                    outcome
                    message

                    RAW LOG:
                    """ + rawLog;

            Map<String, Object> body =
                    new LinkedHashMap<>();

            body.put("model", model);
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("format", "json");

            Map<String, Object> options =
                    new LinkedHashMap<>();

            options.put("temperature", 0);

            body.put("options", options);

            String jsonBody =
                    objectMapper.writeValueAsString(body);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            ollamaUrl
                                                    + "/api/generate"
                                    )
                            )
                            .timeout(
                                    Duration.ofSeconds(60)
                            )
                            .header(
                                    "Content-Type",
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

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Ollama returned HTTP "
                                + response.statusCode()
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            String modelResponse =
                    root.path("response")
                            .asText();

            if (modelResponse == null
                    || modelResponse.isBlank()) {

                throw new RuntimeException(
                        "Ollama returned empty response"
                );
            }

            Map<String, Object> normalized =
                    objectMapper.readValue(
                            modelResponse,
                            new TypeReference<
                                    Map<String, Object>>() {
                            }
                    );

            sanitize(normalized);

            return normalized;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Ollama normalization failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private void sanitize(
            Map<String, Object> fields
    ) {

        removeBlank(fields);

        sanitizePort(
                fields,
                "source_port"
        );

        sanitizePort(
                fields,
                "destination_port"
        );

        normalizeCase(
                fields,
                "severity"
        );

        normalizeCase(
                fields,
                "protocol"
        );

        normalizeCase(
                fields,
                "outcome"
        );
    }

    private void removeBlank(
            Map<String, Object> fields
    ) {

        fields.replaceAll(
                (key, value) -> {

                    if (value == null) {
                        return null;
                    }

                    if (value instanceof String string) {

                        String trimmed =
                                string.trim();

                        if (trimmed.isEmpty()
                                || trimmed.equalsIgnoreCase(
                                        "null"
                                )
                                || trimmed.equalsIgnoreCase(
                                        "unknown"
                                )) {

                            return null;
                        }

                        return trimmed;
                    }

                    return value;
                }
        );
    }

    private void sanitizePort(
            Map<String, Object> fields,
            String key
    ) {

        Object value =
                fields.get(key);

        if (value == null) {
            return;
        }

        try {

            int port =
                    Integer.parseInt(
                            String.valueOf(value)
                    );

            if (port < 0
                    || port > 65535) {

                fields.put(key, null);

            } else {

                fields.put(key, port);
            }

        } catch (Exception e) {

            fields.put(key, null);
        }
    }

    private void normalizeCase(
            Map<String, Object> fields,
            String key
    ) {

        Object value =
                fields.get(key);

        if (value instanceof String string) {

            fields.put(
                    key,
                    string.trim()
                            .toUpperCase()
            );
        }
    }
public List<AiMappingSuggestion> suggestMappings(
        String rawLog,
        Map<String, Object> extractedFields
) {

    Map<String, Object> normalized =
            normalizeLog(rawLog);

    List<AiMappingSuggestion> suggestions =
            new ArrayList<>();

    for (Map.Entry<String, Object> entry
            : normalized.entrySet()) {

        String universalField =
                entry.getKey();

        Object value =
                entry.getValue();

        if (value == null) {
            continue;
        }

        String sourceField =
                extractedFields.containsKey(universalField)
                        ? universalField
                        : "raw_log";

        suggestions.add(
                new AiMappingSuggestion(
                        sourceField,
                        String.valueOf(value),
                        universalField,
                        0.85,
                        "OLLAMA"
                )
        );
    }

    return suggestions;
}
}