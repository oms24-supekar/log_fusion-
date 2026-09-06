package com.ntro.ulpf.service;

import com.ntro.ulpf.dto.AiMappingSuggestion;
import com.ntro.ulpf.dto.FieldMappingSuggestion;
import com.ntro.ulpf.dto.UnknownLogAnalysisResponse;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.repository.RawLogRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UnknownLogStructureAnalyzer {

    private final RawLogRepository rawLogRepository;
    private final AiServiceClient aiServiceClient;

    public UnknownLogStructureAnalyzer(
            RawLogRepository rawLogRepository,
            AiServiceClient aiServiceClient
    ) {
        this.rawLogRepository =
                rawLogRepository;

        this.aiServiceClient =
                aiServiceClient;
    }

    public UnknownLogAnalysisResponse analyze(
            UUID rawLogId
    ) {

        RawLog rawLog =
                rawLogRepository
                        .findById(rawLogId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Raw log not found: "
                                                + rawLogId
                                )
                        );

        String rawContent =
                rawLog.getRawContent();

        String delimiter =
                detectDelimiter(rawContent);

        String keyValueSeparator =
                detectKeyValueSeparator(
                        rawContent,
                        delimiter
                );

        Map<String, Object> extractedFields =
                extractFields(
                        rawContent,
                        delimiter,
                        keyValueSeparator
                );

        String signaturePrefix =
                suggestSignaturePrefix(
                        rawContent,
                        delimiter
                );

        List<FieldMappingSuggestion>
                deterministicSuggestions =
                generateSuggestions(
                        extractedFields
                );

        List<AiMappingSuggestion>
                aiSuggestions;

        String aiStatus;

        try {

            aiSuggestions =
                    aiServiceClient
                            .suggestMappings(
                                    rawContent,
                                    extractedFields
                            );

            aiStatus = "AVAILABLE";

        } catch (Exception e) {

            /*
             * IMPORTANT:
             * AI is an assistant, not a hard dependency.
             *
             * If Python is unavailable, deterministic
             * parsing/review must continue working.
             */

            aiSuggestions =
                    List.of();

            aiStatus = "UNAVAILABLE";

            System.err.println(
                    "AI service unavailable: "
                            + e.getMessage()
            );
        }

        return new UnknownLogAnalysisResponse(
                rawLog.getId(),
                rawContent,
                delimiter,
                keyValueSeparator,
                signaturePrefix,
                extractedFields,
                deterministicSuggestions,
                aiSuggestions,
                aiStatus
        );
    }

    private String detectDelimiter(
            String rawLog
    ) {

        String[] candidates = {
                "|",
                ",",
                ";",
                "\t"
        };

        String bestDelimiter = " ";
        int bestCount = 0;

        for (String candidate : candidates) {

            int count =
                    countOccurrences(
                            rawLog,
                            candidate
                    );

            if (count > bestCount) {

                bestCount = count;
                bestDelimiter = candidate;
            }
        }

        return bestDelimiter;
    }

    private String detectKeyValueSeparator(
            String rawLog,
            String delimiter
    ) {

        String[] candidates = {
                "=",
                ":"
        };

        String[] tokens =
                split(
                        rawLog,
                        delimiter
                );

        String bestSeparator = "=";
        int bestCount = 0;

        for (String candidate : candidates) {

            int count = 0;

            for (String token : tokens) {

                if (token.contains(candidate)) {
                    count++;
                }
            }

            if (count > bestCount) {

                bestCount = count;
                bestSeparator = candidate;
            }
        }

        return bestSeparator;
    }

    private Map<String, Object> extractFields(
            String rawLog,
            String delimiter,
            String keyValueSeparator
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        String[] tokens =
                split(
                        rawLog,
                        delimiter
                );

        for (int i = 0;
             i < tokens.length;
             i++) {

            String token =
                    tokens[i].trim();

            fields.put(
                    "token" + i,
                    token
            );

            int separatorIndex =
                    token.indexOf(
                            keyValueSeparator
                    );

            if (separatorIndex > 0) {

                String key =
                        token.substring(
                                0,
                                separatorIndex
                        ).trim();

                String value =
                        token.substring(
                                separatorIndex
                                        + keyValueSeparator.length()
                        ).trim();

                if (!key.isBlank()) {

                    fields.put(
                            key,
                            value
                    );
                }
            }
        }

        return fields;
    }

    private String suggestSignaturePrefix(
            String rawLog,
            String delimiter
    ) {

        String[] tokens =
                split(
                        rawLog,
                        delimiter
                );

        if (tokens.length == 0) {
            return rawLog;
        }

        String firstToken =
                tokens[0].trim();

        if (" ".equals(delimiter)) {

            return firstToken + " ";
        }

        return firstToken + delimiter;
    }

    private List<FieldMappingSuggestion>
    generateSuggestions(
            Map<String, Object> fields
    ) {

        List<FieldMappingSuggestion>
                suggestions =
                new ArrayList<>();

        for (Map.Entry<String, Object> entry
                : fields.entrySet()) {

            String sourceField =
                    entry.getKey();

            String sampleValue =
                    String.valueOf(
                            entry.getValue()
                    );

            if (sourceField.startsWith("token")) {

                if ("token1".equals(sourceField)
                        && looksLikeAction(
                                sampleValue
                        )) {

                    suggestions.add(
                            new FieldMappingSuggestion(
                                    sourceField,
                                    sampleValue,
                                    "action",
                                    0.75,
                                    "Second positional token resembles an event action"
                            )
                    );
                }

                continue;
            }

            FieldMappingSuggestion suggestion =
                    suggestMapping(
                            sourceField,
                            sampleValue
                    );

            if (suggestion != null) {
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    private FieldMappingSuggestion suggestMapping(
            String sourceField,
            String sampleValue
    ) {

        String key =
                sourceField
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replace(
                                "-",
                                "_"
                        );

        if (matchesAny(
                key,
                "src",
                "source",
                "source_ip",
                "src_ip",
                "client_ip",
                "client"
        )) {

            return new FieldMappingSuggestion(
                    sourceField,
                    sampleValue,
                    "source_ip",
                    0.95,
                    "Field name resembles a source/client address"
            );
        }

        if (matchesAny(
                key,
                "dst",
                "dest",
                "destination",
                "destination_ip",
                "dst_ip",
                "server_ip"
        )) {

            String universalField =
                    looksLikeIp(sampleValue)
                            ? "destination_ip"
                            : "destination_host";

            return new FieldMappingSuggestion(
                    sourceField,
                    sampleValue,
                    universalField,
                    0.92,
                    looksLikeIp(sampleValue)
                            ? "Destination-like field contains an IP address"
                            : "Destination-like field contains a host identifier"
            );
        }

        if (matchesAny(
                key,
                "usr",
                "user",
                "username",
                "account",
                "account_name"
        )) {

            return new FieldMappingSuggestion(
                    sourceField,
                    sampleValue,
                    "username",
                    0.97,
                    "Field name resembles a user/account identifier"
            );
        }

        if (matchesAny(
                key,
                "action",
                "event",
                "event_action",
                "type"
        )) {

            return new FieldMappingSuggestion(
                    sourceField,
                    sampleValue,
                    "action",
                    0.90,
                    "Field name resembles an event action"
            );
        }

        if (matchesAny(
                key,
                "severity",
                "sev",
                "level",
                "priority"
        )) {

            return new FieldMappingSuggestion(
                    sourceField,
                    sampleValue,
                    "severity",
                    0.95,
                    "Field name resembles event severity"
            );
        }

        if (matchesAny(
                key,
                "port",
                "dpt",
                "dst_port",
                "destination_port"
        )) {

            return new FieldMappingSuggestion(
                    sourceField,
                    sampleValue,
                    "destination_port",
                    0.94,
                    "Field name resembles destination port"
            );
        }

        if (matchesAny(
                key,
                "host",
                "hostname",
                "source_host"
        )) {

            return new FieldMappingSuggestion(
                    sourceField,
                    sampleValue,
                    "host",
                    0.90,
                    "Field name resembles a host name"
            );
        }

        if (matchesAny(
                key,
                "timestamp",
                "time",
                "ts",
                "event_time"
        )) {

            return new FieldMappingSuggestion(
                    sourceField,
                    sampleValue,
                    "timestamp",
                    0.90,
                    "Field name resembles event time"
            );
        }

        return null;
    }

    private boolean looksLikeAction(
            String value
    ) {

        if (value == null) {
            return false;
        }

        String normalized =
                value.toUpperCase(
                        Locale.ROOT
                );

        return normalized.contains("AUTH")
                || normalized.contains("LOGIN")
                || normalized.contains("BLOCK")
                || normalized.contains("ALLOW")
                || normalized.contains("DENY")
                || normalized.contains("FAIL")
                || normalized.contains("CONNECT")
                || normalized.contains("DROP");
    }

    private boolean looksLikeIp(
            String value
    ) {

        if (value == null) {
            return false;
        }

        return value.matches(
                "^((25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.|$)){4}$"
        );
    }

    private boolean matchesAny(
            String value,
            String... candidates
    ) {

        for (String candidate : candidates) {

            if (value.equals(candidate)) {
                return true;
            }
        }

        return false;
    }

    private String[] split(
            String rawLog,
            String delimiter
    ) {

        if (" ".equals(delimiter)) {

            return rawLog
                    .trim()
                    .split("\\s+");
        }

        return rawLog.split(
                Pattern.quote(delimiter),
                -1
        );
    }

    private int countOccurrences(
            String text,
            String target
    ) {

        int count = 0;
        int index = 0;

        while ((index =
                text.indexOf(
                        target,
                        index
                )) != -1) {

            count++;

            index += target.length();
        }

        return count;
    }
}