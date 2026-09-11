package com.ntro.ulpf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntro.ulpf.detection.LogFormat;
import com.ntro.ulpf.dto.CreateParserDefinitionRequest;
import com.ntro.ulpf.dto.LogResponse;
import com.ntro.ulpf.entity.NormalizedLog;
import com.ntro.ulpf.entity.ParserCreationMode;
import com.ntro.ulpf.entity.ParserDefinition;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.normalization.NormalizationEngine;
import com.ntro.ulpf.normalization.UniversalEvent;
import com.ntro.ulpf.parser.DynamicParseResult;
import com.ntro.ulpf.parser.ParsedLog;
import com.ntro.ulpf.repository.NormalizedLogRepository;
import com.ntro.ulpf.repository.ParserDefinitionRepository;
import com.ntro.ulpf.repository.RawLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AutomaticNormalizationService {

    private final RawLogRepository rawLogRepository;
    private final NormalizedLogRepository normalizedLogRepository;
    private final ParserDefinitionRepository parserDefinitionRepository;
    private final DynamicParserService dynamicParserService;
    private final NormalizationEngine normalizationEngine;
    private final NormalizationConfidenceService normalizationConfidenceService;
    private final NormalizationValidationService normalizationValidationService;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public AutomaticNormalizationService(
            RawLogRepository rawLogRepository,
            NormalizedLogRepository normalizedLogRepository,
            ParserDefinitionRepository parserDefinitionRepository,
            DynamicParserService dynamicParserService,
            NormalizationEngine normalizationEngine,
            NormalizationConfidenceService normalizationConfidenceService,
            NormalizationValidationService normalizationValidationService,
            AiServiceClient aiServiceClient
    ) {
        this.rawLogRepository = rawLogRepository;
        this.normalizedLogRepository = normalizedLogRepository;
        this.parserDefinitionRepository = parserDefinitionRepository;
        this.dynamicParserService = dynamicParserService;
        this.normalizationEngine = normalizationEngine;
        this.normalizationConfidenceService = normalizationConfidenceService;
        this.normalizationValidationService = normalizationValidationService;
        this.aiServiceClient = aiServiceClient;
    }

    public LogResponse process(RawLog rawLog) {
        if (rawLog == null) {
            throw new IllegalArgumentException("Raw log cannot be null");
        }

        String content = rawLog.getRawContent();
        if (content == null || content.isBlank()) {
            rawLog.setProcessingStatus("FAILED");
            rawLogRepository.save(rawLog);
            return toResponse(rawLog);
        }

        Optional<DynamicParseResult> dynamicResult = dynamicParserService.tryParse(content);
        if (dynamicResult.isPresent()) {
            DynamicParseResult result = dynamicResult.get();
            rawLog.setDetectedFormat(LogFormat.DYNAMIC.name());
            rawLog.setProcessingStatus("DETECTED");
            rawLogRepository.save(rawLog);
            return processParsedLog(rawLog, result.parsedLog(), "Dynamic:" + result.definition().getName());
        }

        StructureAnalysis analysis = analyzeUnknownLog(content);
        Map<String, Object> fields = analysis.fields();
        double overallScore = normalizationConfidenceService.calculate(
                analysis.structureConfidence(),
                analysis.mappingConfidence(),
                fields.size(),
                analysis.conflictCount(),
                analysis.validationPassed()
        );

        if (normalizationConfidenceService.isAiEnabled() && overallScore < 0.95 && overallScore >= 0.70) {
            try {
                aiServiceClient.suggestMappings(content, fields);
            } catch (Exception ignored) {
                // AI is optional for the unknown path; if it fails, keep deterministic rules only.
            }
        }

        if (normalizationConfidenceService.shouldAutoApprove(overallScore)) {
            return autoApprove(rawLog, fields, overallScore, analysis);
        }

        rawLog.setProcessingStatus("NEEDS_REVIEW");
        rawLogRepository.save(rawLog);
        return toResponse(rawLog);
    }

    private LogResponse autoApprove(RawLog rawLog, Map<String, Object> fields, double confidence, StructureAnalysis analysis) {
        try {
            Map<String, String> normalizedMappings = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                normalizedMappings.put(key, value == null ? "" : String.valueOf(value));
            }

            String mappingsJson = objectMapper.writeValueAsString(normalizedMappings);
            ParserDefinition definition = new ParserDefinition(
                    UUID.randomUUID(),
                    "AUTO_" + System.currentTimeMillis(),
                    analysis.signaturePrefix(),
                    analysis.delimiter(),
                    analysis.keyValueSeparator(),
                    mappingsJson,
                    true,
                    confidence,
                    ParserCreationMode.AUTO,
                    0,
                    0,
                    null,
                    LocalDateTime.now()
            );
            parserDefinitionRepository.save(definition);

            ParsedLog parsedLog = new ParsedLog(LogFormat.DYNAMIC, fields);
            LogResponse response = processParsedLog(rawLog, parsedLog, "AUTO:" + definition.getName());
            rawLog.setProcessingStatus("NORMALIZED");
            rawLogRepository.save(rawLog);
            return response;
        } catch (Exception e) {
            rawLog.setProcessingStatus("FAILED");
            rawLogRepository.save(rawLog);
            return toResponse(rawLog);
        }
    }

    private LogResponse processParsedLog(RawLog rawLog, ParsedLog parsedLog, String parserUsed) {
        try {
            var validationResult = normalizationValidationService.validate(parsedLog.fields());
            if (validationResult == NormalizationValidationService.ValidationStatus.INVALID) {
                rawLog.setProcessingStatus("NEEDS_REVIEW");
                rawLogRepository.save(rawLog);
                return toResponse(rawLog);
            }

            UniversalEvent universalEvent = normalizationEngine.normalize(rawLog, parsedLog, parserUsed);
            String normalizedJson = objectMapper.writeValueAsString(universalEvent);
            NormalizedLog normalizedLog = new NormalizedLog(
                    UUID.randomUUID(),
                    rawLog,
                    normalizedJson,
                    parserUsed,
                    universalEvent.metadata().processedAt(),
                    validationResult.name()
            );
            normalizedLogRepository.save(normalizedLog);
            rawLog.setProcessingStatus("NORMALIZED");
            rawLogRepository.save(rawLog);
            return new LogResponse(
                    rawLog.getId(),
                    rawLog.getSourceName(),
                    rawLog.getSourceType(),
                    rawLog.getDetectedFormat(),
                    rawLog.getSha256Hash(),
                    rawLog.getProcessingStatus(),
                    rawLog.getReceivedAt()
            );
        } catch (Exception e) {
            rawLog.setProcessingStatus("FAILED");
            rawLogRepository.save(rawLog);
            return new LogResponse(
                    rawLog.getId(),
                    rawLog.getSourceName(),
                    rawLog.getSourceType(),
                    rawLog.getDetectedFormat(),
                    rawLog.getSha256Hash(),
                    rawLog.getProcessingStatus(),
                    rawLog.getReceivedAt()
            );
        }
    }

    private StructureAnalysis analyzeUnknownLog(String content) {
        String trimmed = content.trim();
        String delimiter = detectDelimiter(trimmed);
        String keyValueSeparator = detectKeyValueSeparator(trimmed);
        String signaturePrefix = detectSignature(trimmed);
        Map<String, Object> fields = extractFields(trimmed, delimiter, keyValueSeparator);

        int recognizedFields = fields.size();
        double structureConfidence = 0.5;
        if (signaturePrefix != null && !signaturePrefix.isBlank()) {
            structureConfidence += 0.15;
        }
        if (delimiter != null && !delimiter.isBlank()) {
            structureConfidence += 0.15;
        }
        if (keyValueSeparator != null && !keyValueSeparator.isBlank()) {
            structureConfidence += 0.15;
        }
        if (recognizedFields >= 4) {
            structureConfidence += 0.10;
        }
        structureConfidence = Math.min(1.0, structureConfidence);

        double mappingConfidence = recognizedFields == 0 ? 0.0 : Math.min(1.0, recognizedFields / 10.0);
        boolean validationPassed = !fields.isEmpty();
        int conflictCount = 0;

        return new StructureAnalysis(
                delimiter,
                keyValueSeparator,
                signaturePrefix,
                fields,
                structureConfidence,
                mappingConfidence,
                conflictCount,
                validationPassed
        );
    }

    private String detectDelimiter(String raw) {
        if (raw.contains("||")) return "||";
        if (raw.contains(";")) return ";";
        if (raw.contains(",")) return ",";
        if (raw.contains("\t")) return "\t";
        if (raw.contains("|")) return "|";
        return " ";
    }

    private String detectKeyValueSeparator(String raw) {
        if (raw.contains("==")) return "==";
        if (raw.contains("=")) return "=";
        if (raw.contains(":")) return ":";
        if (raw.contains("->")) return "->";
        return "=";
    }

    private String detectSignature(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String line = raw.split("\\R", 2)[0].trim();
        if (line.startsWith("CEF:")) return "CEF:";
        if (line.startsWith("LEEF:")) return "LEEF:";
        if (line.startsWith("{") || line.startsWith("[")) return "JSON";
        return line.length() > 0 ? line.substring(0, Math.min(20, line.length())) : "";
    }

    private Map<String, Object> extractFields(String raw, String delimiter, String keyValueSeparator) {
        Map<String, Object> fields = new LinkedHashMap<>();
        String[] lines = raw.split("\\R");

        for (String line : lines) {
            String value = line.trim();
            if (value.isEmpty()) continue;

            if (value.contains(keyValueSeparator)) {
                String[] parts = value.split(java.util.regex.Pattern.quote(keyValueSeparator), 2);
                if (parts.length == 2) {
                    fields.put(parts[0].trim(), parts[1].trim());
                    continue;
                }
            }

            String[] tokens = value.split(java.util.regex.Pattern.quote(delimiter));
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].trim();
                if (!token.isEmpty()) {
                    fields.put("token" + (i + 1), token);
                }
            }
        }

        if (fields.isEmpty()) {
            String[] tokens = raw.split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                if (!tokens[i].trim().isEmpty()) {
                    fields.put("token" + (i + 1), tokens[i].trim());
                }
            }
        }

        return fields;
    }

    private LogResponse toResponse(RawLog rawLog) {
        return new LogResponse(
                rawLog.getId(),
                rawLog.getSourceName(),
                rawLog.getSourceType(),
                rawLog.getDetectedFormat(),
                rawLog.getSha256Hash(),
                rawLog.getProcessingStatus(),
                rawLog.getReceivedAt()
        );
    }

    private record StructureAnalysis(
            String delimiter,
            String keyValueSeparator,
            String signaturePrefix,
            Map<String, Object> fields,
            double structureConfidence,
            double mappingConfidence,
            int conflictCount,
            boolean validationPassed
    ) {
    }
}
