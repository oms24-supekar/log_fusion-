package com.ntro.ulpf.service;

import com.ntro.ulpf.entity.ParserDefinition;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ParserMatchingService {

    public boolean matches(ParserDefinition definition, String rawLog) {
        return scoreMatch(definition, rawLog) >= 0.75;
    }

    public double scoreMatch(ParserDefinition definition, String rawLog) {
        if (definition == null || rawLog == null || rawLog.isBlank()) {
            return 0.0;
        }

        String trimmed = rawLog.trim();
        double score = 0.0;

        if (definition.getSignaturePrefix() != null && !definition.getSignaturePrefix().isBlank()) {
            String signature = definition.getSignaturePrefix();
            if (trimmed.startsWith(signature) || trimmed.contains(signature)) {
                score += 0.45;
            } else if ("__JSON__".equals(signature) && trimmed.startsWith("{")) {
                score += 0.40;
            } else if ("__CEF__".equals(signature) && trimmed.startsWith("CEF:")) {
                score += 0.40;
            } else if ("__LEEF__".equals(signature) && trimmed.startsWith("LEEF:")) {
                score += 0.40;
            }
        }

        if (definition.getDelimiter() != null && !definition.getDelimiter().isBlank()) {
            String delimiter = definition.getDelimiter();
            if (trimmed.contains(delimiter) || " ".equals(delimiter) && trimmed.matches(".*\\s+.*")) {
                score += 0.20;
            }
        }

        if (definition.getKeyValueSeparator() != null && !definition.getKeyValueSeparator().isBlank()) {
            String sep = definition.getKeyValueSeparator();
            if (trimmed.contains(sep) || trimmed.matches(".*\\s+[A-Za-z0-9_.-]+\\s*=\\s*.*")) {
                score += 0.20;
            }
        }

        if (definition.getConfidence() != null) {
            score += definition.getConfidence() * 0.15;
        }

        if (definition.isEnabled()) {
            score += 0.05;
        }

        return Math.min(1.0, Math.max(0.0, score));
    }
}
