package com.ntro.ulpf.service;

import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

@Service
public class NormalizationValidationService {

    public enum ValidationStatus {
        VALID,
        PARTIAL,
        INVALID
    }

    private static final Set<String> VALID_SEVERITIES = Set.of(
            "CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO", "WARNING", "ERROR", "FATAL", "UNKNOWN"
    );

    private static final Set<String> VALID_PROTOCOLS = Set.of(
            "TCP", "UDP", "ICMP", "HTTP", "HTTPS", "DNS", "FTP", "SMTP", "SSH", "TLS", "UNKNOWN"
    );

    public ValidationStatus validate(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return ValidationStatus.INVALID;
        }

        int invalidChecks = 0;
        int partialChecks = 0;

        if (!hasRequiredStructure(fields)) {
            invalidChecks++;
        }

        if (hasField(fields, "timestamp")) {
            if (!isValidTimestamp(getString(fields, "timestamp"))) {
                invalidChecks++;
            }
        } else {
            partialChecks++;
        }

        if (hasField(fields, "source_ip") && !isValidIp(getString(fields, "source_ip"))) {
            invalidChecks++;
        }

        if (hasField(fields, "destination_ip") && !isValidIp(getString(fields, "destination_ip"))) {
            invalidChecks++;
        }

        if (hasField(fields, "destination_port")) {
            Object portValue = fields.get("destination_port");
            if (!isValidPort(portValue)) {
                invalidChecks++;
            }
        }

        if (hasField(fields, "severity")) {
            String severity = getString(fields, "severity");
            if (!VALID_SEVERITIES.contains(severity.toUpperCase())) {
                invalidChecks++;
            }
        }

        if (hasField(fields, "protocol")) {
            String protocol = getString(fields, "protocol");
            if (!VALID_PROTOCOLS.contains(protocol.toUpperCase())) {
                invalidChecks++;
            }
        }

        if (invalidChecks > 0) {
            return ValidationStatus.INVALID;
        }

        if (partialChecks > 0 || fields.size() < 3) {
            return ValidationStatus.PARTIAL;
        }

        return ValidationStatus.VALID;
    }

    private boolean hasRequiredStructure(Map<String, Object> fields) {
        return fields.size() > 0 && fields.values().stream().anyMatch(value -> value != null && value.toString().trim().length() > 0);
    }

    private boolean hasField(Map<String, Object> fields, String key) {
        return fields.containsKey(key) && fields.get(key) != null && !fields.get(key).toString().isBlank();
    }

    private String getString(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean isValidTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            LocalDateTime.parse(value);
            return true;
        } catch (DateTimeParseException ignored) {
        }

        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (DateTimeParseException ignored) {
        }

        try {
            DateTimeFormatter.ISO_INSTANT.parse(value);
            return true;
        } catch (DateTimeParseException ignored) {
        }

        return false;
    }

    private boolean isValidIp(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            InetAddress.getByName(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidPort(Object value) {
        if (value == null) {
            return true;
        }

        try {
            int port = Integer.parseInt(String.valueOf(value));
            return port >= 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
