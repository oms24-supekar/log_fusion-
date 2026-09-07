package com.ntro.ulpf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntro.ulpf.dto.AiMappingSuggestion;
import com.ntro.ulpf.dto.FieldMappingSuggestion;
import com.ntro.ulpf.dto.UnknownLogAnalysisResponse;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.repository.RawLogRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UnknownLogStructureAnalyzer {

    private static final String TYPE_SPRING = "__SPRING_BOOT__";
    private static final String TYPE_RFC3164 = "__RFC3164_SYSLOG__";
    private static final String TYPE_RFC5424 = "__RFC5424_SYSLOG__";
    private static final String TYPE_ACCESS = "__ACCESS_LOG__";
    private static final String TYPE_WINDOWS_XML = "__WINDOWS_XML__";
    private static final String TYPE_JSON = "__JSON__";
    private static final String TYPE_CEF = "__CEF__";
    private static final String TYPE_LEEF = "__LEEF__";

    private static final Pattern SPRING_LOG_PATTERN =
            Pattern.compile(
                    "^(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?)\\s+" +
                    "(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL)\\s+" +
                    "(?:\\[[^]]*]\\s*)?" +
                    "([^\\s]+)\\s+-\\s+(.*)$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern RFC3164_PATTERN =
            Pattern.compile(
                    "^(?:<\\d{1,3}>)?" +
                    "([A-Z][a-z]{2}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+" +
                    "(\\S+)\\s+" +
                    "([^:\\s]+)(?:\\[(\\d+)])?:\\s*(.*)$"
            );

    private static final Pattern RFC5424_PATTERN =
            Pattern.compile(
                    "^<\\d{1,3}>\\d\\s+" +
                    "(\\S+)\\s+" +
                    "(\\S+)\\s+" +
                    "(\\S+)\\s+" +
                    "(\\S+)\\s+" +
                    "(\\S+)\\s+" +
                    "(?:-|\\[[^]]*])\\s*(.*)$"
            );

    private static final Pattern ACCESS_LOG_PATTERN =
            Pattern.compile(
                    "^(\\S+)\\s+" +
                    "(\\S+)\\s+" +
                    "(\\S+)\\s+" +
                    "\\[([^]]+)]\\s+" +
                    "\"([A-Z]+)\\s+([^\\s\"]+)(?:\\s+HTTP/([^\"]+))?\"\\s+" +
                    "(\\d{3})\\s+" +
                    "(\\S+)" +
                    "(?:\\s+\"([^\"]*)\")?" +
                    "(?:\\s+\"([^\"]*)\")?.*$"
            );

    private static final Pattern IPV4_PATTERN =
            Pattern.compile(
                    "\\b(?:(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}" +
                    "(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\b"
            );

    private static final Pattern EMAIL_OR_USER_PATTERN =
            Pattern.compile(
                    "(?i)\\b(?:user|username|account|identity|principal)\\s*[=:]?\\s*([A-Za-z0-9._@\\\\-]+)"
            );

    private static final Pattern PORT_PATTERN =
            Pattern.compile(
                    "(?i)\\b(?:port|dpt|dst_port|destination_port|service_port|target_port)\\s*[=:]?\\s*(\\d{1,5})"
            );

    private final RawLogRepository rawLogRepository;
    private final AiServiceClient aiServiceClient;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    public UnknownLogStructureAnalyzer(
            RawLogRepository rawLogRepository,
            AiServiceClient aiServiceClient
    ) {
        this.rawLogRepository = rawLogRepository;
        this.aiServiceClient = aiServiceClient;
    }

    public UnknownLogAnalysisResponse analyze(
            UUID rawLogId
    ) {

        RawLog rawLog =
                rawLogRepository
                        .findById(rawLogId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Raw log not found: " + rawLogId
                                )
                        );

        String rawContent =
                rawLog.getRawContent();

        if (rawContent == null ||
                rawContent.isBlank()) {

            throw new IllegalArgumentException(
                    "Raw log content is empty"
            );
        }

        StructureResult structure =
                analyzeStructure(rawContent.trim());

        List<FieldMappingSuggestion>
                deterministicSuggestions =
                generateSuggestions(
                        structure.fields()
                );

        List<AiMappingSuggestion>
                aiSuggestions;

        String aiStatus;

        try {

            aiSuggestions =
                    aiServiceClient
                            .suggestMappings(
                                    rawContent,
                                    structure.fields()
                            );

            aiStatus = "AVAILABLE";

        } catch (Exception e) {

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
                structure.delimiter(),
                structure.keyValueSeparator(),
                structure.signature(),
                structure.fields(),
                deterministicSuggestions,
                aiSuggestions,
                aiStatus
        );
    }

    private StructureResult analyzeStructure(
            String rawLog
    ) {

        StructureResult result;

        result = tryJson(rawLog);
        if (result != null) {
            return result;
        }

        result = tryCef(rawLog);
        if (result != null) {
            return result;
        }

        result = tryLeef(rawLog);
        if (result != null) {
            return result;
        }

        result = trySpringBoot(rawLog);
        if (result != null) {
            return result;
        }

        result = tryRfc5424(rawLog);
        if (result != null) {
            return result;
        }

        result = tryRfc3164(rawLog);
        if (result != null) {
            return result;
        }

        result = tryAccessLog(rawLog);
        if (result != null) {
            return result;
        }

        result = tryWindowsXml(rawLog);
        if (result != null) {
            return result;
        }

        return tryGenericStructuredLog(rawLog);
    }

    private StructureResult tryJson(
            String rawLog
    ) {

        try {

            JsonNode node =
                    objectMapper.readTree(rawLog);

            if (node == null ||
                    !node.isObject()) {

                return null;
            }

            Map<String, Object> fields =
                    new LinkedHashMap<>();

            flattenJson(
                    "",
                    node,
                    fields
            );

            return new StructureResult(
                    TYPE_JSON,
                    ":",
                    TYPE_JSON,
                    fields
            );

        } catch (Exception ignored) {

            return null;
        }
    }

    private StructureResult trySpringBoot(
            String rawLog
    ) {

        Matcher matcher =
                SPRING_LOG_PATTERN.matcher(
                        firstLine(rawLog)
                );

        if (!matcher.matches()) {

            return null;
        }

        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "timestamp",
                matcher.group(1)
        );

        fields.put(
                "severity",
                matcher.group(2)
        );

        fields.put(
                "process",
                matcher.group(3)
        );

        String message =
                matcher.group(4);

        fields.put(
                "message",
                message
        );

        enrichFromMessage(
                message,
                fields
        );

        return new StructureResult(
                TYPE_SPRING,
                "=",
                TYPE_SPRING,
                fields
        );
    }

    private StructureResult tryRfc3164(
            String rawLog
    ) {

        Matcher matcher =
                RFC3164_PATTERN.matcher(
                        firstLine(rawLog)
                );

        if (!matcher.matches()) {

            return null;
        }

        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "timestamp",
                matcher.group(1)
        );

        fields.put(
                "host",
                matcher.group(2)
        );

        fields.put(
                "process",
                matcher.group(3)
        );

        if (matcher.group(4) != null) {

            fields.put(
                    "process_id",
                    matcher.group(4)
            );
        }

        String message =
                matcher.group(5);

        fields.put(
                "message",
                message
        );

        enrichFromMessage(
                message,
                fields
        );

        return new StructureResult(
                TYPE_RFC3164,
                ":",
                TYPE_RFC3164,
                fields
        );
    }

    private StructureResult tryRfc5424(
            String rawLog
    ) {

        Matcher matcher =
                RFC5424_PATTERN.matcher(
                        firstLine(rawLog)
                );

        if (!matcher.matches()) {

            return null;
        }

        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "timestamp",
                matcher.group(1)
        );

        fields.put(
                "host",
                matcher.group(2)
        );

        fields.put(
                "process",
                matcher.group(3)
        );

        fields.put(
                "process_id",
                matcher.group(4)
        );

        fields.put(
                "event_id",
                matcher.group(5)
        );

        String message =
                matcher.group(6);

        fields.put(
                "message",
                message
        );

        enrichFromMessage(
                message,
                fields
        );

        return new StructureResult(
                TYPE_RFC5424,
                "=",
                TYPE_RFC5424,
                fields
        );
    }

    private StructureResult tryAccessLog(
            String rawLog
    ) {

        Matcher matcher =
                ACCESS_LOG_PATTERN.matcher(
                        firstLine(rawLog)
                );

        if (!matcher.matches()) {

            return null;
        }

        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "source_ip",
                matcher.group(1)
        );

        fields.put(
                "username",
                "-".equals(matcher.group(3))
                        ? null
                        : matcher.group(3)
        );

        fields.put(
                "timestamp",
                matcher.group(4)
        );

        fields.put(
                "action",
                matcher.group(5)
        );

        fields.put(
                "request_path",
                matcher.group(6)
        );

        fields.put(
                "protocol",
                matcher.group(7) == null
                        ? "HTTP"
                        : "HTTP/" + matcher.group(7)
        );

        fields.put(
                "status_code",
                matcher.group(8)
        );

        fields.put(
                "bytes",
                matcher.group(9)
        );

        if (matcher.group(10) != null) {

            fields.put(
                    "referrer",
                    matcher.group(10)
            );
        }

        if (matcher.group(11) != null) {

            fields.put(
                    "user_agent",
                    matcher.group(11)
            );
        }

        fields.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue() == null
                );

        return new StructureResult(
                TYPE_ACCESS,
                " ",
                TYPE_ACCESS,
                fields
        );
    }

    private StructureResult tryWindowsXml(
            String rawLog
    ) {

        String trimmed =
                rawLog.trim();

        if (!trimmed.startsWith("<") ||
                !trimmed.toLowerCase(Locale.ROOT)
                        .contains("<event")) {

            return null;
        }

        Map<String, Object> fields =
                new LinkedHashMap<>();

        putXmlValue(
                fields,
                "event_id",
                trimmed,
                "EventID"
        );

        putXmlValue(
                fields,
                "computer",
                trimmed,
                "Computer"
        );

        putXmlAttribute(
                fields,
                "timestamp",
                trimmed,
                "SystemTime"
        );

        putXmlAttribute(
                fields,
                "provider",
                trimmed,
                "Name"
        );

        Pattern dataPattern =
                Pattern.compile(
                        "<Data\\s+Name=[\"']([^\"']+)[\"'][^>]*>(.*?)</Data>",
                        Pattern.CASE_INSENSITIVE |
                                Pattern.DOTALL
                );

        Matcher dataMatcher =
                dataPattern.matcher(trimmed);

        while (dataMatcher.find()) {

            String key =
                    sanitizeKey(
                            dataMatcher.group(1)
                    );

            String value =
                    stripXml(
                            dataMatcher.group(2)
                    );

            if (!key.isBlank() &&
                    !value.isBlank()) {

                fields.put(
                        key,
                        value
                );
            }
        }

        return new StructureResult(
                TYPE_WINDOWS_XML,
                "=",
                TYPE_WINDOWS_XML,
                fields
        );
    }

    private StructureResult tryCef(
            String rawLog
    ) {

        if (!rawLog.startsWith("CEF:")) {

            return null;
        }

        String[] parts =
                rawLog.split(
                        "\\|",
                        8
                );

        if (parts.length < 7) {

            return null;
        }

        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "cef_version",
                parts[0]
        );

        fields.put(
                "vendor",
                parts[1]
        );

        fields.put(
                "product",
                parts[2]
        );

        fields.put(
                "product_version",
                parts[3]
        );

        fields.put(
                "event_id",
                parts[4]
        );

        fields.put(
                "action",
                parts[5]
        );

        fields.put(
                "severity",
                parts[6]
        );

        if (parts.length == 8) {

            fields.putAll(
                    extractWhitespaceKeyValues(
                            parts[7]
                    )
            );
        }

        return new StructureResult(
                TYPE_CEF,
                "=",
                TYPE_CEF,
                fields
        );
    }

    private StructureResult tryLeef(
            String rawLog
    ) {

        if (!rawLog.startsWith("LEEF:")) {

            return null;
        }

        String[] parts =
                rawLog.split(
                        "\\|",
                        6
                );

        if (parts.length < 5) {

            return null;
        }

        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "leef_version",
                parts[0]
        );

        fields.put(
                "vendor",
                parts[1]
        );

        fields.put(
                "product",
                parts[2]
        );

        fields.put(
                "product_version",
                parts[3]
        );

        fields.put(
                "event_id",
                parts[4]
        );

        if (parts.length == 6) {

            String extension =
                    parts[5];

            String delimiter =
                    extension.contains("\t")
                            ? "\t"
                            : extension.contains("|")
                            ? "|"
                            : " ";

            fields.putAll(
                    extractKeyValueTokens(
                            extension,
                            delimiter,
                            "="
                    )
            );
        }

        return new StructureResult(
                TYPE_LEEF,
                "=",
                TYPE_LEEF,
                fields
        );
    }

    private StructureResult tryGenericStructuredLog(
            String rawLog
    ) {

        String delimiter =
                detectDelimiter(rawLog);

        String separator =
                detectKeyValueSeparator(
                        rawLog,
                        delimiter
                );

        Map<String, Object> fields;

        if (" ".equals(delimiter)) {

            Map<String, Object> keyValues =
                    extractWhitespaceKeyValues(
                            rawLog
                    );

            if (keyValues.size() >= 2) {

                fields =
                        new LinkedHashMap<>(
                                keyValues
                        );

                addPositionalActionIfPresent(
                        rawLog,
                        fields
                );

            } else {

                fields =
                        extractGenericTokens(
                                rawLog,
                                delimiter,
                                separator
                        );
            }

        } else {

            fields =
                    extractGenericTokens(
                            rawLog,
                            delimiter,
                            separator
                    );
        }

        String signature =
                suggestVendorSignature(
                        rawLog,
                        delimiter
                );

        return new StructureResult(
                delimiter,
                separator,
                signature,
                fields
        );
    }

    private Map<String, Object>
    extractGenericTokens(
            String rawLog,
            String delimiter,
            String separator
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

            if (token.isBlank()) {
                continue;
            }

            fields.put(
                    "token" + i,
                    token
            );

            int separatorIndex =
                    token.indexOf(
                            separator
                    );

            if (separatorIndex > 0) {

                String key =
                        sanitizeKey(
                                token.substring(
                                        0,
                                        separatorIndex
                                )
                        );

                String value =
                        token.substring(
                                separatorIndex
                                        + separator.length()
                        ).trim();

                if (!key.isBlank() &&
                        !value.isBlank()) {

                    fields.put(
                            key,
                            stripQuotes(value)
                    );
                }
            }
        }

        return fields;
    }

    private Map<String, Object>
    extractWhitespaceKeyValues(
            String text
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        Pattern pattern =
                Pattern.compile(
                        "(?<!\\S)([A-Za-z_][A-Za-z0-9_.-]*)=" +
                        "(\"[^\"]*\"|'[^']*'|\\S+)"
                );

        Matcher matcher =
                pattern.matcher(text);

        while (matcher.find()) {

            fields.put(
                    sanitizeKey(
                            matcher.group(1)
                    ),
                    stripQuotes(
                            matcher.group(2)
                    )
            );
        }

        return fields;
    }

    private Map<String, Object>
    extractKeyValueTokens(
            String text,
            String delimiter,
            String separator
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        String[] tokens =
                split(
                        text,
                        delimiter
                );

        for (String token : tokens) {

            int index =
                    token.indexOf(
                            separator
                    );

            if (index <= 0) {
                continue;
            }

            String key =
                    sanitizeKey(
                            token.substring(
                                    0,
                                    index
                            )
                    );

            String value =
                    token.substring(
                            index
                                    + separator.length()
                    ).trim();

            if (!key.isBlank()) {

                fields.put(
                        key,
                        stripQuotes(value)
                );
            }
        }

        return fields;
    }

    private void flattenJson(
            String prefix,
            JsonNode node,
            Map<String, Object> fields
    ) {

        Iterator<Map.Entry<String, JsonNode>>
                iterator =
                node.fields();

        while (iterator.hasNext()) {

            Map.Entry<String, JsonNode>
                    entry =
                    iterator.next();

            String key =
                    prefix.isBlank()
                            ? entry.getKey()
                            : prefix
                            + "_"
                            + entry.getKey();

            JsonNode value =
                    entry.getValue();

            if (value.isObject()) {

                flattenJson(
                        key,
                        value,
                        fields
                );

            } else if (value.isValueNode()) {

                fields.put(
                        sanitizeKey(key),
                        value.isTextual()
                                ? value.asText()
                                : value.toString()
                );
            }
        }
    }

    private void enrichFromMessage(
            String message,
            Map<String, Object> fields
    ) {

        if (message == null ||
                message.isBlank()) {

            return;
        }

        Matcher ipMatcher =
                IPV4_PATTERN.matcher(
                        message
                );

        if (ipMatcher.find()) {

            fields.putIfAbsent(
                    "source_ip",
                    ipMatcher.group()
            );

            if (ipMatcher.find()) {

                fields.putIfAbsent(
                        "destination_ip",
                        ipMatcher.group()
                );
            }
        }

        Matcher userMatcher =
                EMAIL_OR_USER_PATTERN.matcher(
                        message
                );

        if (userMatcher.find()) {

            fields.putIfAbsent(
                    "username",
                    userMatcher.group(1)
            );
        }

        Matcher portMatcher =
                PORT_PATTERN.matcher(
                        message
                );

        if (portMatcher.find()) {

            fields.putIfAbsent(
                    "destination_port",
                    portMatcher.group(1)
            );
        }

        String upper =
                message.toUpperCase(
                        Locale.ROOT
                );

        if (looksLikeAction(upper)) {

            fields.putIfAbsent(
                    "action",
                    detectAction(upper)
            );
        }

        if (upper.contains("FAIL") ||
                upper.contains("DENY") ||
                upper.contains("BLOCK") ||
                upper.contains("REJECT")) {

            fields.putIfAbsent(
                    "outcome",
                    "FAILED"
            );

        } else if (upper.contains("SUCCESS") ||
                upper.contains("ALLOW") ||
                upper.contains("ACCEPT")) {

            fields.putIfAbsent(
                    "outcome",
                    "SUCCESS"
            );
        }
    }

    private void addPositionalActionIfPresent(
            String rawLog,
            Map<String, Object> fields
    ) {

        String[] tokens =
                rawLog.trim()
                        .split("\\s+");

        for (int i = 0;
             i < Math.min(
                     tokens.length,
                     4
             );
             i++) {

            if (looksLikeAction(
                    tokens[i]
            )) {

                fields.putIfAbsent(
                        "token" + i,
                        tokens[i]
                );

                break;
            }
        }
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

                if (looksLikeAction(
                        sampleValue
                )) {

                    suggestions.add(
                            new FieldMappingSuggestion(
                                    sourceField,
                                    sampleValue,
                                    "action",
                                    0.90,
                                    "Positional token resembles an event action"
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

                suggestions.add(
                        suggestion
                );
            }
        }

        return suggestions;
    }

    private FieldMappingSuggestion suggestMapping(
            String sourceField,
            String sampleValue
    ) {

        String key =
                sanitizeKey(
                        sourceField
                );

        if (matchesAny(
                key,
                "src",
                "src_ip",
                "source",
                "source_ip",
                "source_address",
                "client",
                "client_ip",
                "client_addr",
                "client_address",
                "origin",
                "origin_ip",
                "origin_addr",
                "origin_address"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "source_ip",
                    0.99,
                    "Source/client/origin address"
            );
        }

        if (matchesAny(
                key,
                "dst",
                "dst_ip",
                "dest",
                "destination",
                "destination_ip",
                "destination_address",
                "target",
                "target_ip",
                "target_addr",
                "target_address",
                "remote",
                "remote_ip",
                "remote_addr",
                "remote_address",
                "remote_host",
                "server_ip"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    looksLikeIp(sampleValue)
                            ? "destination_ip"
                            : "destination_host",
                    0.98,
                    "Destination/target/remote address"
            );
        }

        if (matchesAny(
                key,
                "user",
                "usr",
                "username",
                "user_name",
                "account",
                "account_name",
                "identity",
                "principal",
                "login",
                "subject_user_name"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "username",
                    0.99,
                    "User/account identity"
            );
        }

        if (matchesAny(
                key,
                "action",
                "act",
                "event",
                "event_action",
                "operation",
                "method",
                "verb",
                "event_type",
                "type"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "action",
                    0.96,
                    "Event action or operation"
            );
        }

        if (matchesAny(
                key,
                "severity",
                "sev",
                "level",
                "priority",
                "risk",
                "risk_level",
                "threat",
                "threat_level",
                "log_level"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "severity",
                    0.98,
                    "Severity or risk level"
            );
        }

        if (matchesAny(
                key,
                "destination_port",
                "dst_port",
                "dpt",
                "port",
                "service_port",
                "target_port",
                "remote_port"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "destination_port",
                    0.98,
                    "Destination/service port"
            );
        }

        if (matchesAny(
                key,
                "source_port",
                "src_port",
                "spt",
                "client_port",
                "origin_port"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "source_port",
                    0.98,
                    "Source/client port"
            );
        }

        if (matchesAny(
                key,
                "host",
                "hostname",
                "source_host",
                "computer",
                "device",
                "device_name",
                "machine"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "host",
                    0.96,
                    "Source host/device"
            );
        }

        if (matchesAny(
                key,
                "destination_host",
                "dst_host",
                "target_host",
                "remote_host",
                "server",
                "server_host"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "destination_host",
                    0.96,
                    "Destination host"
            );
        }

        if (matchesAny(
                key,
                "timestamp",
                "time",
                "ts",
                "event_time",
                "datetime",
                "date_time",
                "event_timestamp",
                "systemtime"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "timestamp",
                    0.99,
                    "Event timestamp"
            );
        }

        if (matchesAny(
                key,
                "protocol",
                "proto",
                "transport",
                "network_protocol"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "protocol",
                    0.97,
                    "Network/application protocol"
            );
        }

        if (matchesAny(
                key,
                "process",
                "process_name",
                "application",
                "app",
                "logger",
                "class",
                "component",
                "service"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "process",
                    0.96,
                    "Process/application/logger"
            );
        }

        if (matchesAny(
                key,
                "outcome",
                "result",
                "status",
                "disposition",
                "decision"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "outcome",
                    0.94,
                    "Event outcome or result"
            );
        }

        if (matchesAny(
                key,
                "message",
                "msg",
                "description",
                "detail",
                "details",
                "reason",
                "text"
        )) {

            return suggestion(
                    sourceField,
                    sampleValue,
                    "message",
                    0.97,
                    "Human-readable event message"
            );
        }

        return null;
    }

    private FieldMappingSuggestion suggestion(
            String sourceField,
            String sampleValue,
            String target,
            double confidence,
            String reason
    ) {

        return new FieldMappingSuggestion(
                sourceField,
                sampleValue,
                target,
                confidence,
                reason
        );
    }

    private String detectDelimiter(
            String rawLog
    ) {

        String[] candidates = {
                "|",
                ";",
                "\t",
                ","
        };

        String bestDelimiter = " ";
        int bestScore = 0;

        for (String candidate : candidates) {

            int count =
                    countOccurrences(
                            rawLog,
                            candidate
                    );

            if (count > bestScore) {

                bestScore = count;
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

    private String suggestVendorSignature(
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

        String first =
                tokens[0].trim();

        if (!" ".equals(delimiter) &&
                first.matches(
                        "[A-Za-z][A-Za-z0-9_.-]{1,80}"
                )) {

            return first + delimiter;
        }

        return first + " ";
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
                || normalized.contains("LOGOUT")
                || normalized.contains("BLOCK")
                || normalized.contains("ALLOW")
                || normalized.contains("DENY")
                || normalized.contains("FAIL")
                || normalized.contains("CONNECT")
                || normalized.contains("DROP")
                || normalized.contains("REJECT")
                || normalized.contains("ACCEPT")
                || normalized.contains("CREATE")
                || normalized.contains("DELETE")
                || normalized.contains("UPDATE")
                || normalized.contains("READ")
                || normalized.equals("GET")
                || normalized.equals("POST")
                || normalized.equals("PUT")
                || normalized.equals("PATCH");
    }

    private String detectAction(
            String upper
    ) {

        String[] actions = {
                "LOGIN_FAILED",
                "LOGIN_SUCCESS",
                "INTRUSION_BLOCK",
                "ACCESS_ALLOW",
                "ACCESS_DENY",
                "AUTH_FAIL",
                "AUTH_SUCCESS",
                "BLOCK",
                "ALLOW",
                "DENY",
                "DROP",
                "REJECT",
                "ACCEPT",
                "LOGIN",
                "LOGOUT",
                "CONNECT",
                "CREATE",
                "DELETE",
                "UPDATE"
        };

        for (String action : actions) {

            if (upper.contains(action)) {
                return action;
            }
        }

        if (upper.contains("FAILED PASSWORD")) {
            return "LOGIN_FAILED";
        }

        if (upper.contains("AUTHENTICATION FAILED")) {
            return "AUTH_FAIL";
        }

        if (upper.contains("AUTHENTICATION SUCCESS")) {
            return "AUTH_SUCCESS";
        }

        return "EVENT";
    }

    private boolean looksLikeIp(
            String value
    ) {

        return value != null &&
                IPV4_PATTERN
                        .matcher(value)
                        .matches();
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

    private String sanitizeKey(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^a-z0-9]+",
                        "_"
                )
                .replaceAll(
                        "^_+|_+$",
                        ""
                );
    }

    private String stripQuotes(
            String value
    ) {

        if (value == null ||
                value.length() < 2) {

            return value;
        }

        if ((value.startsWith("\"") &&
                value.endsWith("\""))
                ||
                (value.startsWith("'") &&
                        value.endsWith("'"))) {

            return value.substring(
                    1,
                    value.length() - 1
            );
        }

        return value;
    }

    private String stripXml(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replaceAll(
                        "<[^>]+>",
                        ""
                )
                .trim();
    }

    private void putXmlValue(
            Map<String, Object> fields,
            String key,
            String xml,
            String tag
    ) {

        Pattern pattern =
                Pattern.compile(
                        "<" + tag +
                                "(?:\\s[^>]*)?>(.*?)</" +
                                tag + ">",
                        Pattern.CASE_INSENSITIVE |
                                Pattern.DOTALL
                );

        Matcher matcher =
                pattern.matcher(xml);

        if (matcher.find()) {

            fields.put(
                    key,
                    stripXml(
                            matcher.group(1)
                    )
            );
        }
    }

    private void putXmlAttribute(
            Map<String, Object> fields,
            String key,
            String xml,
            String attribute
    ) {

        Pattern pattern =
                Pattern.compile(
                        attribute +
                                "=[\"']([^\"']+)[\"']",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher matcher =
                pattern.matcher(xml);

        if (matcher.find()) {

            fields.put(
                    key,
                    matcher.group(1)
            );
        }
    }

    private String firstLine(
            String rawLog
    ) {

        int newline =
                rawLog.indexOf('\n');

        if (newline < 0) {
            return rawLog.trim();
        }

        return rawLog
                .substring(
                        0,
                        newline
                )
                .trim();
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

    private record StructureResult(
            String delimiter,
            String keyValueSeparator,
            String signature,
            Map<String, Object> fields
    ) {
    }
}