package com.ntro.ulpf.normalization;

import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.parser.ParsedLog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class NormalizationEngine {

    private final FieldMapper fieldMapper;

    public NormalizationEngine(FieldMapper fieldMapper) {
        this.fieldMapper = fieldMapper;
    }

    public UniversalEvent normalize(
            RawLog rawLog,
            ParsedLog parsedLog,
            String parserUsed
    ) {

        Map<String, Object> fields =
                parsedLog.fields();

        String sourceIp = fieldMapper.getString(
                fields,
                "source_ip",
                "src",
                "sourceIp",
                "client_ip",
                "clientIp"
        );

        String sourceHost = fieldMapper.getString(
                fields,
                "host",
                "source_host",
                "sourceHost",
                "hostname"
        );

        String destinationIp = fieldMapper.getString(
                fields,
                "destination_ip",
                "dst",
                "dest_ip",
                "destinationIp",
                "server_ip"
        );

        String destinationHost = fieldMapper.getString(
                fields,
                "destination_host",
                "dest_host",
                "destinationHost"
        );

        Integer destinationPort = fieldMapper.getInteger(
                fields,
                "destination_port",
                "dpt",
                "dest_port",
                "port"
        );

        String username = fieldMapper.getString(
                fields,
                "username",
                "user",
                "usr",
                "account"
        );

        String timestamp = fieldMapper.getString(
                fields,
                "timestamp",
                "time",
                "event_time",
                "@timestamp"
        );

        String action = fieldMapper.getString(
                fields,
                "action",
                "event_action",
                "name"
        );

        String rawSeverity = fieldMapper.getString(
                fields,
                "severity",
                "level",
                "priority"
        );

        String severity =
                normalizeSeverity(rawSeverity);

        String category =
                determineCategory(fields, action);

        String outcome =
                determineOutcome(fields, action);

        UniversalEvent.Source source =
                new UniversalEvent.Source(
                        sourceIp,
                        sourceHost,
                        rawLog.getSourceType()
                );

        UniversalEvent.Destination destination =
                new UniversalEvent.Destination(
                        destinationIp,
                        destinationHost,
                        destinationPort
                );

        UniversalEvent.User user =
                new UniversalEvent.User(
                        username
                );

        UniversalEvent.Event event =
                new UniversalEvent.Event(
                        category,
                        normalizeAction(action),
                        outcome,
                        severity
                );

        UniversalEvent.Metadata metadata =
                new UniversalEvent.Metadata(
                        parsedLog.format().name(),
                        parserUsed,
                        LocalDateTime.now()
                );

        UniversalEvent.RawReference rawReference =
                new UniversalEvent.RawReference(
                        rawLog.getId(),
                        rawLog.getSha256Hash()
                );

        return new UniversalEvent(
                UUID.randomUUID(),
                timestamp,
                source,
                destination,
                user,
                event,
                metadata,
                rawReference
        );
    }

    private String normalizeAction(String action) {

        if (action == null || action.isBlank()) {
            return "UNKNOWN";
        }

        return action
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    private String normalizeSeverity(String severity) {

        if (severity == null || severity.isBlank()) {
            return "UNKNOWN";
        }

        String value =
                severity.trim().toUpperCase(Locale.ROOT);

        try {

            int numericSeverity =
                    Integer.parseInt(value);

            if (numericSeverity >= 8) {
                return "CRITICAL";
            }

            if (numericSeverity >= 6) {
                return "HIGH";
            }

            if (numericSeverity >= 3) {
                return "MEDIUM";
            }

            return "LOW";

        } catch (NumberFormatException ignored) {
        }

        return switch (value) {
            case "EMERGENCY",
                 "ALERT",
                 "CRITICAL",
                 "FATAL" -> "CRITICAL";

            case "ERROR",
                 "ERR",
                 "HIGH" -> "HIGH";

            case "WARNING",
                 "WARN",
                 "MEDIUM" -> "MEDIUM";

            case "INFO",
                 "INFORMATIONAL",
                 "DEBUG",
                 "LOW" -> "LOW";

            default -> "UNKNOWN";
        };
    }

    private String determineCategory(
            Map<String, Object> fields,
            String action
    ) {

        String message = fieldMapper.getString(
                fields,
                "message"
        );

        String combined =
                ((action == null ? "" : action)
                        + " "
                        + (message == null ? "" : message))
                        .toLowerCase(Locale.ROOT);

        if (combined.contains("login")
                || combined.contains("password")
                || combined.contains("auth")) {

            return "AUTHENTICATION";
        }

        if (combined.contains("blocked")
                || combined.contains("denied")
                || combined.contains("connection")
                || fields.containsKey("src")
                || fields.containsKey("dst")) {

            return "NETWORK";
        }

        return "GENERAL";
    }

    private String determineOutcome(
            Map<String, Object> fields,
            String action
    ) {

        String explicitOutcome =
                fieldMapper.getString(
                        fields,
                        "outcome",
                        "result",
                        "status"
                );

        if (explicitOutcome != null) {
            return explicitOutcome
                    .toUpperCase(Locale.ROOT);
        }

        String message =
                fieldMapper.getString(
                        fields,
                        "message"
                );

        String combined =
                ((action == null ? "" : action)
                        + " "
                        + (message == null ? "" : message))
                        .toLowerCase(Locale.ROOT);

        if (combined.contains("fail"))
    return "FAILED";

        if (combined.contains("blocked")
                || combined.contains("denied")) {

            return "DENIED";
        }

        if (combined.contains("success")
                || combined.contains("accepted")
                || combined.contains("allowed")) {

            return "SUCCESS";
        }

        return "UNKNOWN";
    }
}