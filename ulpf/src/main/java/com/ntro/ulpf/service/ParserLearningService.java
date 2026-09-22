package com.ntro.ulpf.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntro.ulpf.entity.ParserLearningCandidate;
import com.ntro.ulpf.parser.ParsedLog;
import com.ntro.ulpf.repository.ParserLearningCandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ParserLearningService {

    private static final long READY_MIN_EXAMPLES = 5;
    private static final double READY_MIN_CONFIDENCE = 0.90;

    private final ParserLearningCandidateRepository candidateRepository;
    private final LogStructureFingerprintService fingerprintService;
    private final ParserPromotionService parserPromotionService;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    public ParserLearningService(
            ParserLearningCandidateRepository candidateRepository,
            LogStructureFingerprintService fingerprintService,
            ParserPromotionService parserPromotionService
    ) {
        this.candidateRepository = candidateRepository;
        this.fingerprintService = fingerprintService;
        this.parserPromotionService = parserPromotionService;
    }

    @Transactional
    public void learnFromSuccessfulAiNormalization(
            String rawLog,
            ParsedLog parsedLog
    ) {
        if (rawLog == null || rawLog.isBlank()
                || parsedLog == null
                || parsedLog.fields() == null
                || parsedLog.fields().isEmpty()) {
            return;
        }

        var fingerprint = fingerprintService.analyze(rawLog);

        Map<String, String> incomingMappings =
                inferMappings(
                        rawLog,
                        fingerprint.delimiter(),
                        fingerprint.keyValueSeparator(),
                        parsedLog.fields()
                );

        Optional<ParserLearningCandidate> existing =
                candidateRepository.findByFingerprint(
                        fingerprint.fingerprint()
                );

        ParserLearningCandidate candidate =
                existing.isPresent()
                        ? updateExistingCandidate(
                                existing.get(),
                                incomingMappings
                        )
                        : createCandidate(
                                rawLog,
                                fingerprint,
                                incomingMappings
                        );

        if ("READY".equals(candidate.getStatus())) {
            parserPromotionService.promoteIfReady(
                    candidate.getId()
            );
        }
    }

    private ParserLearningCandidate createCandidate(
            String rawLog,
            LogStructureFingerprintService.StructureFingerprint fingerprint,
            Map<String, String> mappings
    ) {
        try {
            LocalDateTime now = LocalDateTime.now();

            ParserLearningCandidate candidate =
                    new ParserLearningCandidate(
                            UUID.randomUUID(),
                            fingerprint.fingerprint(),
                            fingerprint.signaturePrefix(),
                            fingerprint.delimiter(),
                            fingerprint.keyValueSeparator(),
                            objectMapper.writeValueAsString(mappings),
                            1,
                            1,
                            0,
                            1.0,
                            "LEARNING",
                            null,
                            rawLog,
                            now,
                            now
                    );

            return candidateRepository.save(candidate);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to create parser learning candidate",
                    e
            );
        }
    }

    private ParserLearningCandidate updateExistingCandidate(
            ParserLearningCandidate candidate,
            Map<String, String> incomingMappings
    ) {
        try {
            Map<String, String> existingMappings =
                    readMappings(candidate.getFieldMappings());

            boolean conflict =
                    hasConflict(
                            existingMappings,
                            incomingMappings
                    );

            candidate.setExamplesSeen(
                    candidate.getExamplesSeen() + 1
            );

            if (conflict) {
                candidate.setConflictingExamples(
                        candidate.getConflictingExamples() + 1
                );
            } else {
                candidate.setSuccessfulExamples(
                        candidate.getSuccessfulExamples() + 1
                );

                mergeMappings(
                        existingMappings,
                        incomingMappings
                );

                candidate.setFieldMappings(
                        objectMapper.writeValueAsString(
                                existingMappings
                        )
                );
            }

            double confidence =
                    calculateConfidence(candidate);

            candidate.setConfidence(confidence);
            candidate.setLastSeenAt(LocalDateTime.now());

            boolean ready =
                    candidate.getSuccessfulExamples()
                            >= READY_MIN_EXAMPLES
                    && confidence >= READY_MIN_CONFIDENCE
                    && candidate.getConflictingExamples() == 0
                    && !existingMappings.isEmpty();

            candidate.setStatus(
                    ready ? "READY" : "LEARNING"
            );

            return candidateRepository.save(candidate);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to update parser learning candidate",
                    e
            );
        }
    }

    private Map<String, String> inferMappings(
            String rawLog,
            String delimiter,
            String keyValueSeparator,
            Map<String, Object> aiFields
    ) {
        Map<String, String> mappings =
                new LinkedHashMap<>();

        String[] tokens = split(rawLog, delimiter);

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();

            if (token.isBlank()) {
                continue;
            }

            int separatorIndex =
                    keyValueSeparator == null
                            || keyValueSeparator.isBlank()
                            ? -1
                            : token.indexOf(keyValueSeparator);

            if (separatorIndex > 0) {
                String rawKey =
                        sanitizeKey(
                                token.substring(
                                        0,
                                        separatorIndex
                                )
                        );

                String rawValue =
                        stripQuotes(
                                token.substring(
                                        separatorIndex
                                                + keyValueSeparator.length()
                                ).trim()
                        );

                String aiField =
                        findMatchingAiField(
                                rawValue,
                                aiFields
                        );

                if (aiField != null) {
                    mappings.put(rawKey, aiField);
                }

                continue;
            }

            String aiField =
                    findMatchingAiField(
                            token,
                            aiFields
                    );

            if (aiField != null) {
                mappings.put(
                        "token" + i,
                        aiField
                );
            }
        }

        return mappings;
    }

    private String findMatchingAiField(
            String rawValue,
            Map<String, Object> aiFields
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalizedRaw = normalizeValue(rawValue);

        for (Map.Entry<String, Object> entry
                : aiFields.entrySet()) {

            if (entry.getValue() == null) {
                continue;
            }

            String normalizedAi =
                    normalizeValue(
                            String.valueOf(entry.getValue())
                    );

            if (normalizedRaw.equals(normalizedAi)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private boolean hasConflict(
            Map<String, String> existing,
            Map<String, String> incoming
    ) {
        for (Map.Entry<String, String> entry
                : incoming.entrySet()) {

            String old = existing.get(entry.getKey());

            if (old != null
                    && !old.equals(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    private void mergeMappings(
            Map<String, String> existing,
            Map<String, String> incoming
    ) {
        incoming.forEach(existing::putIfAbsent);
    }

    private double calculateConfidence(
            ParserLearningCandidate candidate
    ) {
        long total =
                candidate.getSuccessfulExamples()
                        + candidate.getConflictingExamples();

        if (total <= 0) {
            return 0.0;
        }

        return (double)
                candidate.getSuccessfulExamples()
                / total;
    }

    private Map<String, String> readMappings(
            String json
    ) {
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, String>>() {}
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to read candidate mappings",
                    e
            );
        }
    }

    private String[] split(
            String rawLog,
            String delimiter
    ) {
        if (delimiter == null
                || delimiter.isBlank()
                || " ".equals(delimiter)) {
            return rawLog.trim().split("\\s+");
        }

        return rawLog.split(
                Pattern.quote(delimiter),
                -1
        );
    }

    private String sanitizeKey(
            String value
    ) {
        return value
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String normalizeValue(
            String value
    ) {
        return stripQuotes(value)
                .trim()
                .toLowerCase();
    }

    private String stripQuotes(
            String value
    ) {
        if (value == null || value.length() < 2) {
            return value;
        }

        if ((value.startsWith("\"")
                && value.endsWith("\""))
                || (value.startsWith("'")
                && value.endsWith("'"))) {

            return value.substring(
                    1,
                    value.length() - 1
            );
        }

        return value;
    }
}
