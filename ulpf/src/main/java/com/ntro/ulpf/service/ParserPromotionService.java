package com.ntro.ulpf.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntro.ulpf.dto.CreateParserDefinitionRequest;
import com.ntro.ulpf.dto.ParserDefinitionResponse;
import com.ntro.ulpf.entity.ParserCreationMode;
import com.ntro.ulpf.entity.ParserLearningCandidate;
import com.ntro.ulpf.repository.ParserLearningCandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ParserPromotionService {

    private static final long MIN_EXAMPLES = 5;
    private static final double MIN_CONFIDENCE = 0.90;

    private final ParserLearningCandidateRepository
            candidateRepository;

    private final DynamicParserService
            dynamicParserService;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    public ParserPromotionService(
            ParserLearningCandidateRepository candidateRepository,
            DynamicParserService dynamicParserService
    ) {
        this.candidateRepository = candidateRepository;
        this.dynamicParserService = dynamicParserService;
    }

    @Transactional
    public void promoteIfReady(
            UUID candidateId
    ) {
        ParserLearningCandidate candidate =
                candidateRepository
                        .findById(candidateId)
                        .orElse(null);

        if (candidate == null) {
            return;
        }

        if (!"READY".equals(candidate.getStatus())) {
            return;
        }

        if (candidate.getPromotedParserId() != null) {
            return;
        }

        if (candidate.getSuccessfulExamples() < MIN_EXAMPLES
                || candidate.getConfidence() < MIN_CONFIDENCE
                || candidate.getConflictingExamples() > 0) {
            return;
        }

        try {
            Map<String, String> mappings =
                    objectMapper.readValue(
                            candidate.getFieldMappings(),
                            new TypeReference<Map<String, String>>() {}
                    );

            if (mappings.isEmpty()) {
                return;
            }

            String parserName =
                    "AUTO_"
                            + candidate.getFingerprint()
                            .substring(0, 12);

            CreateParserDefinitionRequest request =
                    new CreateParserDefinitionRequest(
                            parserName,
                            candidate.getSignaturePrefix(),
                            candidate.getDelimiter(),
                            candidate.getKeyValueSeparator(),
                            mappings,
                            true
                    );

            ParserDefinitionResponse parser =
                    dynamicParserService.createDefinition(
                            request,
                            ParserCreationMode.AUTO
                    );

            dynamicParserService.testDefinition(
                    parser.id(),
                    candidate.getSampleRawLog()
            );

            candidate.setPromotedParserId(
                    parser.id()
            );

            candidate.setStatus(
                    "PROMOTED"
            );

            candidateRepository.save(candidate);

            System.out.println(
                    "AUTO parser promoted: "
                            + parser.name()
                            + " from candidate "
                            + candidate.getId()
            );

        } catch (Exception e) {
            System.err.println(
                    "Parser promotion failed for "
                            + candidateId
                            + ": "
                            + e.getMessage()
            );
        }
    }
}
