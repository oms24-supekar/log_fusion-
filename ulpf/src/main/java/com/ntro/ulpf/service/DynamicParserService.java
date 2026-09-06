package com.ntro.ulpf.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ntro.ulpf.detection.LogFormat;
import com.ntro.ulpf.dto.CreateParserDefinitionRequest;
import com.ntro.ulpf.dto.DynamicParserTestResponse;
import com.ntro.ulpf.dto.ParserDefinitionResponse;
import com.ntro.ulpf.entity.ParserDefinition;
import com.ntro.ulpf.parser.DynamicParseResult;
import com.ntro.ulpf.parser.ParsedLog;
import com.ntro.ulpf.repository.ParserDefinitionRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DynamicParserService {

    private final ParserDefinitionRepository parserDefinitionRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    public DynamicParserService(
            ParserDefinitionRepository parserDefinitionRepository
    ) {
        this.parserDefinitionRepository =
                parserDefinitionRepository;
    }

    public ParserDefinitionResponse createDefinition(
            CreateParserDefinitionRequest request
    ) {

        try {

            String mappingsJson =
                    objectMapper.writeValueAsString(
                            request.fieldMappings()
                    );

            ParserDefinition definition =
                    new ParserDefinition(
                            UUID.randomUUID(),
                            request.name(),
                            request.signaturePrefix(),
                            request.delimiter(),
                            request.keyValueSeparator(),
                            mappingsJson,
                            request.enabled(),
                            LocalDateTime.now()
                    );

            ParserDefinition saved =
                    parserDefinitionRepository.save(definition);

            return toResponse(saved);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to create parser definition",
                    e
            );
        }
    }

    public List<ParserDefinitionResponse> getAllDefinitions() {

        return parserDefinitionRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DynamicParserTestResponse testDefinition(
            UUID definitionId,
            String rawLog
    ) {

        ParserDefinition definition =
                parserDefinitionRepository
                        .findById(definitionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Parser definition not found: "
                                                + definitionId
                                )
                        );

        ParsedLog parsedLog =
                parseWithDefinition(
                        definition,
                        rawLog,
                        false
                );

        return new DynamicParserTestResponse(
                definition.getId(),
                definition.getName(),
                parsedLog.format().name(),
                parsedLog.fields()
        );
    }

    public Optional<DynamicParseResult> tryParse(
            String rawLog
    ) {

        List<ParserDefinition> definitions =
                parserDefinitionRepository
                        .findByEnabledTrueOrderByCreatedAtDesc();

        for (ParserDefinition definition : definitions) {

            if (matches(definition, rawLog)) {

                ParsedLog parsedLog =
                        parseWithDefinition(
                                definition,
                                rawLog,
                                true
                        );

                return Optional.of(
                        new DynamicParseResult(
                                definition,
                                parsedLog
                        )
                );
            }
        }

        return Optional.empty();
    }

    private boolean matches(
            ParserDefinition definition,
            String rawLog
    ) {

        if (rawLog == null) {
            return false;
        }

        return rawLog
                .trim()
                .startsWith(
                        definition.getSignaturePrefix()
                );
    }

    private ParsedLog parseWithDefinition(
            ParserDefinition definition,
            String rawLog,
            boolean requireSignatureMatch
    ) {

        if (rawLog == null || rawLog.isBlank()) {

            throw new IllegalArgumentException(
                    "Raw log cannot be empty"
            );
        }

        if (requireSignatureMatch
                && !matches(definition, rawLog)) {

            throw new IllegalArgumentException(
                    "Log does not match parser signature"
            );
        }

        Map<String, Object> extractedFields =
                extractFields(
                        rawLog,
                        definition.getDelimiter(),
                        definition.getKeyValueSeparator()
                );

        Map<String, String> mappings =
                readMappings(
                        definition.getFieldMappings()
                );

        Map<String, Object> finalFields =
                new LinkedHashMap<>(
                        extractedFields
                );

        for (Map.Entry<String, String> mapping
                : mappings.entrySet()) {

            Object sourceValue =
                    extractedFields.get(
                            mapping.getKey()
                    );

            if (sourceValue != null) {

                finalFields.put(
                        mapping.getValue(),
                        sourceValue
                );
            }
        }

        return new ParsedLog(
                LogFormat.DYNAMIC,
                finalFields
        );
    }

    private Map<String, Object> extractFields(
            String rawLog,
            String delimiter,
            String keyValueSeparator
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        String[] tokens =
                rawLog.trim().split(
                        Pattern.quote(delimiter),
                        -1
                );

        for (int i = 0; i < tokens.length; i++) {

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

    private Map<String, String> readMappings(
            String mappingsJson
    ) {

        try {

            return objectMapper.readValue(
                    mappingsJson,
                    new TypeReference<Map<String, String>>() {
                    }
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to read parser field mappings",
                    e
            );
        }
    }

    private ParserDefinitionResponse toResponse(
            ParserDefinition definition
    ) {

        return new ParserDefinitionResponse(
                definition.getId(),
                definition.getName(),
                definition.getSignaturePrefix(),
                definition.getDelimiter(),
                definition.getKeyValueSeparator(),
                readMappings(
                        definition.getFieldMappings()
                ),
                definition.isEnabled(),
                definition.getCreatedAt()
        );
    }
}