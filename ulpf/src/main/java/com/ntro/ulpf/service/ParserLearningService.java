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

@Service
public class ParserLearningService {

    private static final long READY_MIN_EXAMPLES = 5;

    private static final double READY_MIN_CONFIDENCE = 0.90;

    private final ParserLearningCandidateRepository
            candidateRepository;

    private final LogStructureFingerprintService
            fingerprintService;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    public ParserLearningService(
            ParserLearningCandidateRepository candidateRepository,
            LogStructureFingerprintService fingerprintService
    ) {
        this.candidateRepository =
                candidateRepository;

        this.fingerprintService =
                fingerprintService;
    }

    @Transactional
    public void learnFromSuccessfulAiNormalization(
            String rawLog,
            ParsedLog parsedLog
    ) {

        if (rawLog == null
                || rawLog.isBlank()
                || parsedLog == null
                || parsedLog.fields() == null
                || parsedLog.fields().isEmpty()) {

            return;
        }

        var fingerprint =
                fingerprintService.analyze(
                        rawLog
                );

        Map<String, String> learnedMappings =
                inferMappings(
                        parsedLog.fields()
                );

        Optional<ParserLearningCandidate> existing =
                candidateRepository
                        .findByFingerprint(
                                fingerprint.fingerprint()
                        );

        if (existing.isPresent()) {

            updateExistingCandidate(
                    existing.get(),
                    learnedMappings
            );

        } else {

            createCandidate(
                    rawLog,
                    fingerprint,
                    learnedMappings
            );
        }
    }

    private void createCandidate(
            String rawLog,
            LogStructureFingerprintService.StructureFingerprint fingerprint,
            Map<String, String> mappings
    ) {

        try {

            LocalDateTime now =
                    LocalDateTime.now();

            String mappingsJson =
                    objectMapper.writeValueAsString(
                            mappings
                    );

            ParserLearningCandidate candidate =
                    new ParserLearningCandidate(
                            UUID.randomUUID(),
                            fingerprint.fingerprint(),
                            fingerprint.signaturePrefix(),
                            fingerprint.delimiter(),
                            fingerprint.keyValueSeparator(),
                            mappingsJson,
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

            candidateRepository.save(
                    candidate
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to create parser learning candidate",
                    e
            );
        }
    }

    private void updateExistingCandidate(
            ParserLearningCandidate candidate,
            Map<String, String> newMappings
    ) {

        try {

            Map<String, String> existingMappings =
                    readMappings(
                            candidate.getFieldMappings()
                    );

            boolean conflict =
                    hasConflict(
                            existingMappings,
                            newMappings
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
                        newMappings
                );

                candidate.setFieldMappings(
                        objectMapper.writeValueAsString(
                                existingMappings
                        )
                );
            }

            double confidence =
                    calculateConfidence(
                            candidate
                    );

            candidate.setConfidence(
                    confidence
            );

            candidate.setLastSeenAt(
                    LocalDateTime.now()
            );

            if (
                    candidate.getSuccessfulExamples()
                            >= READY_MIN_EXAMPLES
                    &&
                    confidence
                            >= READY_MIN_CONFIDENCE
                    &&
                    candidate.getConflictingExamples()
                            == 0
            ) {

                candidate.setStatus(
                        "READY"
                );

            } else {

                candidate.setStatus(
                        "LEARNING"
                );
            }

            candidateRepository.save(
                    candidate
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to update parser learning candidate",
                    e
            );
        }
    }

    private Map<String, String> inferMappings(
            Map<String, Object> aiFields
    ) {

        Map<String, String> mappings =
                new LinkedHashMap<>();

        for (String field
                : aiFields.keySet()) {

            if (field == null
                    || field.isBlank()) {

                continue;
            }

            String normalizedField =
                    field.trim();

            /*
             * AI output is already using LogFusion's
             * universal field names.
             *
             * For now we learn identity mappings.
             *
             * Later Chunk 4 will improve this by learning
             * raw-source-key -> universal-field mappings.
             */
            mappings.put(
                    normalizedField,
                    normalizedField
            );
        }

        return mappings;
    }

    private boolean hasConflict(
            Map<String, String> existing,
            Map<String, String> incoming
    ) {

        for (Map.Entry<String, String> entry
                : incoming.entrySet()) {

            String existingValue =
                    existing.get(
                            entry.getKey()
                    );

            if (existingValue != null
                    &&
                    !existingValue.equals(
                            entry.getValue()
                    )) {

                return true;
            }
        }

        return false;
    }

    private void mergeMappings(
            Map<String, String> existing,
            Map<String, String> incoming
    ) {

        for (Map.Entry<String, String> entry
                : incoming.entrySet()) {

            existing.putIfAbsent(
                    entry.getKey(),
                    entry.getValue()
            );
        }
    }

    private double calculateConfidence(
            ParserLearningCandidate candidate
    ) {

        long total =
                candidate.getSuccessfulExamples()
                        +
                candidate.getConflictingExamples();

        if (total <= 0) {
            return 0.0;
        }

        return (double)
                candidate.getSuccessfulExamples()
                /
                total;
    }

    private Map<String, String> readMappings(
            String json
    ) {

        try {

            return objectMapper.readValue(
                    json,
                    new TypeReference<
                            Map<String, String>
                            >() {
                    }
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to read learning mappings",
                    e
            );
        }
    }
}