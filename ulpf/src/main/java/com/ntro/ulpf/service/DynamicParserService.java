package com.ntro.ulpf.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DynamicParserService {

    private static final String TYPE_SPRING =
            "__SPRING_BOOT__";

    private static final String TYPE_RFC3164 =
            "__RFC3164_SYSLOG__";

    private static final String TYPE_RFC5424 =
            "__RFC5424_SYSLOG__";

    private static final String TYPE_ACCESS =
            "__ACCESS_LOG__";

    private static final String TYPE_WINDOWS_XML =
            "__WINDOWS_XML__";

    private static final String TYPE_JSON =
            "__JSON__";

    private static final String TYPE_CEF =
            "__CEF__";

    private static final String TYPE_LEEF =
            "__LEEF__";

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

    private static final Pattern USER_PATTERN =
            Pattern.compile(
                    "(?i)\\b(?:user|username|account|identity|principal)\\s*[=:]?\\s*([A-Za-z0-9._@\\\\-]+)"
            );

    private static final Pattern PORT_PATTERN =
            Pattern.compile(
                    "(?i)\\b(?:port|dpt|dst_port|destination_port|service_port|target_port)\\s*[=:]?\\s*(\\d{1,5})"
            );

    private final ParserDefinitionRepository
            parserDefinitionRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    public DynamicParserService(
            ParserDefinitionRepository
                    parserDefinitionRepository
    ) {

        this.parserDefinitionRepository =
                parserDefinitionRepository;
    }

    public ParserDefinitionResponse createDefinition(
            CreateParserDefinitionRequest request
    ) {
        return createDefinition(
                request,
                com.ntro.ulpf.entity.ParserCreationMode.MANUAL
        );
    }

    public ParserDefinitionResponse createDefinition(
            CreateParserDefinitionRequest request,
            com.ntro.ulpf.entity.ParserCreationMode creationMode
    ) {

        try {

            String mappingsJson =
                    objectMapper
                            .writeValueAsString(
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
                            0.0,
                            creationMode,
                            0,
                            0,
                            null,
                            LocalDateTime.now()
                    );

            ParserDefinition saved =
                    parserDefinitionRepository
                            .save(definition);

            return toResponse(saved);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to create parser definition",
                    e
            );
        }
    }

    public List<ParserDefinitionResponse>
    getAllDefinitions() {

        return parserDefinitionRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DynamicParserTestResponse
    testDefinition(
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

    public Optional<DynamicParseResult>
    tryParse(
            String rawLog
    ) {

        List<ParserDefinition> definitions =
                parserDefinitionRepository
                        .findByEnabledTrueOrderByCreatedAtDesc();

        for (ParserDefinition definition
                : definitions) {

            if (matches(
                    definition,
                    rawLog
            )) {

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

        if (rawLog == null ||
                rawLog.isBlank()) {

            return false;
        }

        String signature =
                definition
                        .getSignaturePrefix();

        String trimmed =
                rawLog.trim();

        if (TYPE_SPRING.equals(signature)) {

            return SPRING_LOG_PATTERN
                    .matcher(
                            firstLine(trimmed)
                    )
                    .matches();
        }

        if (TYPE_RFC3164.equals(signature)) {

            return RFC3164_PATTERN
                    .matcher(
                            firstLine(trimmed)
                    )
                    .matches();
        }

        if (TYPE_RFC5424.equals(signature)) {

            return RFC5424_PATTERN
                    .matcher(
                            firstLine(trimmed)
                    )
                    .matches();
        }

        if (TYPE_ACCESS.equals(signature)) {

            return ACCESS_LOG_PATTERN
                    .matcher(
                            firstLine(trimmed)
                    )
                    .matches();
        }

        if (TYPE_WINDOWS_XML.equals(signature)) {

            return trimmed
                    .toLowerCase(
                            Locale.ROOT
                    )
                    .contains("<event");
        }

        if (TYPE_JSON.equals(signature)) {

            return looksLikeJsonObject(
                    trimmed
            );
        }

        if (TYPE_CEF.equals(signature)) {

            return trimmed
                    .startsWith("CEF:");
        }

        if (TYPE_LEEF.equals(signature)) {

            return trimmed
                    .startsWith("LEEF:");
        }

        return trimmed
                .startsWith(signature);
    }

    private ParsedLog parseWithDefinition(
            ParserDefinition definition,
            String rawLog,
            boolean requireSignatureMatch
    ) {

        if (rawLog == null ||
                rawLog.isBlank()) {

            throw new IllegalArgumentException(
                    "Raw log cannot be empty"
            );
        }

        if (requireSignatureMatch &&
                !matches(
                        definition,
                        rawLog
                )) {

            throw new IllegalArgumentException(
                    "Log does not match parser signature"
            );
        }

        Map<String, Object> extractedFields =
                extractFields(
                        rawLog.trim(),
                        definition
                );

        Map<String, String> mappings =
                readMappings(
                        definition
                                .getFieldMappings()
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

    private Map<String, Object>
    extractFields(
            String rawLog,
            ParserDefinition definition
    ) {

        String signature =
                definition
                        .getSignaturePrefix();

        if (TYPE_SPRING.equals(signature)) {

            return parseSpring(
                    rawLog
            );
        }

        if (TYPE_RFC3164.equals(signature)) {

            return parseRfc3164(
                    rawLog
            );
        }

        if (TYPE_RFC5424.equals(signature)) {

            return parseRfc5424(
                    rawLog
            );
        }

        if (TYPE_ACCESS.equals(signature)) {

            return parseAccessLog(
                    rawLog
            );
        }

        if (TYPE_WINDOWS_XML.equals(signature)) {

            return parseWindowsXml(
                    rawLog
            );
        }

        if (TYPE_JSON.equals(signature)) {

            return parseJson(
                    rawLog
            );
        }

        if (TYPE_CEF.equals(signature)) {

            return parseCef(
                    rawLog
            );
        }

        if (TYPE_LEEF.equals(signature)) {

            return parseLeef(
                    rawLog
            );
        }

        return parseGeneric(
                rawLog,
                definition.getDelimiter(),
                definition
                        .getKeyValueSeparator()
        );
    }

    private Map<String, Object>
    parseSpring(
            String rawLog
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        Matcher matcher =
                SPRING_LOG_PATTERN.matcher(
                        firstLine(rawLog)
                );

        if (!matcher.matches()) {

            return fields;
        }

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

        enrichMessage(
                message,
                fields
        );

        return fields;
    }

    private Map<String, Object>
    parseRfc3164(
            String rawLog
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        Matcher matcher =
                RFC3164_PATTERN.matcher(
                        firstLine(rawLog)
                );

        if (!matcher.matches()) {

            return fields;
        }

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

        enrichMessage(
                message,
                fields
        );

        return fields;
    }

    private Map<String, Object>
    parseRfc5424(
            String rawLog
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        Matcher matcher =
                RFC5424_PATTERN.matcher(
                        firstLine(rawLog)
                );

        if (!matcher.matches()) {

            return fields;
        }

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

        enrichMessage(
                message,
                fields
        );

        return fields;
    }

    private Map<String, Object>
    parseAccessLog(
            String rawLog
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        Matcher matcher =
                ACCESS_LOG_PATTERN.matcher(
                        firstLine(rawLog)
                );

        if (!matcher.matches()) {

            return fields;
        }

        fields.put(
                "source_ip",
                matcher.group(1)
        );

        if (!"-".equals(
                matcher.group(3)
        )) {

            fields.put(
                    "username",
                    matcher.group(3)
            );
        }

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
                        : "HTTP/"
                        + matcher.group(7)
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

        return fields;
    }

    private Map<String, Object>
    parseWindowsXml(
            String rawLog
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        putXmlValue(
                fields,
                "event_id",
                rawLog,
                "EventID"
        );

        putXmlValue(
                fields,
                "computer",
                rawLog,
                "Computer"
        );

        putXmlAttribute(
                fields,
                "timestamp",
                rawLog,
                "SystemTime"
        );

        putXmlAttribute(
                fields,
                "provider",
                rawLog,
                "Name"
        );

        Pattern dataPattern =
                Pattern.compile(
                        "<Data\\s+Name=[\"']([^\"']+)[\"'][^>]*>(.*?)</Data>",
                        Pattern.CASE_INSENSITIVE |
                                Pattern.DOTALL
                );

        Matcher matcher =
                dataPattern.matcher(
                        rawLog
                );

        while (matcher.find()) {

            String key =
                    sanitizeKey(
                            matcher.group(1)
                    );

            String value =
                    matcher.group(2)
                            .replaceAll(
                                    "<[^>]+>",
                                    ""
                            )
                            .trim();

            if (!key.isBlank() &&
                    !value.isBlank()) {

                fields.put(
                        key,
                        value
                );
            }
        }

        return fields;
    }

    private Map<String, Object>
    parseJson(
            String rawLog
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        try {

            JsonNode root =
                    objectMapper
                            .readTree(rawLog);

            if (root != null &&
                    root.isObject()) {

                flattenJson(
                        "",
                        root,
                        fields
                );
            }

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to parse JSON dynamic log",
                    e
            );
        }

        return fields;
    }

    private Map<String, Object>
    parseCef(
            String rawLog
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        String[] parts =
                rawLog.split(
                        "\\|",
                        8
                );

        if (parts.length < 7) {

            return fields;
        }

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

        return fields;
    }

    private Map<String, Object>
    parseLeef(
            String rawLog
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        String[] parts =
                rawLog.split(
                        "\\|",
                        6
                );

        if (parts.length < 5) {

            return fields;
        }

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

            if (extension.contains("\t")) {

                fields.putAll(
                        parseKeyValueTokens(
                                extension,
                                "\t",
                                "="
                        )
                );

            } else {

                fields.putAll(
                        extractWhitespaceKeyValues(
                                extension
                        )
                );
            }
        }

        return fields;
    }

    private Map<String, Object>
    parseGeneric(
            String rawLog,
            String delimiter,
            String separator
    ) {

        Map<String, Object> fields =
                new LinkedHashMap<>();

        if (" ".equals(delimiter)) {

            Map<String, Object> kv =
                    extractWhitespaceKeyValues(
                            rawLog
                    );

            if (kv.size() >= 2) {

                fields.putAll(kv);

                return fields;
            }
        }

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
                        stripQuotes(
                                token.substring(
                                        separatorIndex
                                                + separator.length()
                                ).trim()
                        );

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
    parseKeyValueTokens(
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

            fields.put(
                    sanitizeKey(
                            token.substring(
                                    0,
                                    index
                            )
                    ),
                    stripQuotes(
                            token.substring(
                                    index
                                            + separator.length()
                            ).trim()
                    )
            );
        }

        return fields;
    }

    private void enrichMessage(
            String message,
            Map<String, Object> fields
    ) {

        if (message == null) {
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
                USER_PATTERN.matcher(
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

        if (upper.contains("FAILED PASSWORD") ||
                upper.contains("AUTHENTICATION FAILED")) {

            fields.putIfAbsent(
                    "action",
                    "LOGIN_FAILED"
            );

        } else if (upper.contains("LOGIN")) {

            fields.putIfAbsent(
                    "action",
                    "LOGIN"
            );

        } else if (upper.contains("BLOCK")) {

            fields.putIfAbsent(
                    "action",
                    "BLOCK"
            );

        } else if (upper.contains("DENY")) {

            fields.putIfAbsent(
                    "action",
                    "DENY"
            );

        } else if (upper.contains("ALLOW")) {

            fields.putIfAbsent(
                    "action",
                    "ALLOW"
            );
        }
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

    private boolean looksLikeJsonObject(
            String rawLog
    ) {

        try {

            JsonNode node =
                    objectMapper
                            .readTree(rawLog);

            return node != null &&
                    node.isObject();

        } catch (Exception ignored) {

            return false;
        }
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
                    matcher.group(1)
                            .replaceAll(
                                    "<[^>]+>",
                                    ""
                            )
                            .trim()
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

    private Map<String, String>
    readMappings(
            String mappingsJson
    ) {

        try {

            return objectMapper
                    .readValue(
                            mappingsJson,
                            new TypeReference<
                                    Map<String, String>
                                    >() {
                            }
                    );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to read parser field mappings",
                    e
            );
        }
    }

    private ParserDefinitionResponse
    toResponse(
            ParserDefinition definition
    ) {

        return new ParserDefinitionResponse(
                definition.getId(),
                definition.getName(),
                definition
                        .getSignaturePrefix(),
                definition.getDelimiter(),
                definition
                        .getKeyValueSeparator(),
                readMappings(
                        definition
                                .getFieldMappings()
                ),
                definition.isEnabled(),
                definition.getConfidence(),
                definition.getCreationMode() == null ? "MANUAL" : definition.getCreationMode().name(),
                definition.getCreatedAt()
        );
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
                Pattern.quote(
                        delimiter
                ),
                -1
        );
    }
}