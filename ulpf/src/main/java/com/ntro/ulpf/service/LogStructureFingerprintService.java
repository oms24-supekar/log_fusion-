package com.ntro.ulpf.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class LogStructureFingerprintService {

    /*
     * =========================================================
     * REGEX PATTERNS
     * =========================================================
     */

    private static final Pattern IPV4_PATTERN =
            Pattern.compile(
                    "\\b(?:(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}" +
                    "(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\b"
            );

    private static final Pattern UUID_PATTERN =
            Pattern.compile(
                    "\\b[0-9a-fA-F]{8}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{12}\\b"
            );

    private static final Pattern ISO_TIMESTAMP_PATTERN =
            Pattern.compile(
                    "\\b\\d{4}-\\d{2}-\\d{2}" +
                    "[T ]" +
                    "\\d{2}:\\d{2}:\\d{2}" +
                    "(?:\\.\\d+)?" +
                    "(?:Z|[+-]\\d{2}:?\\d{2})?\\b"
            );

    private static final Pattern SYSLOG_TIMESTAMP_PATTERN =
            Pattern.compile(
                    "\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)" +
                    "\\s+\\d{1,2}\\s+" +
                    "\\d{2}:\\d{2}:\\d{2}\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile(
                    "(?<![A-Za-z])\\b\\d+\\b(?![A-Za-z])"
            );

    private static final Pattern HEX_PATTERN =
            Pattern.compile(
                    "\\b0x[0-9a-fA-F]+\\b"
            );

    /*
     * Normalize identifiers with changing numeric suffixes.
     *
     * node1  -> node<num>
     * node42 -> node<num>
     * user7  -> user<num>
     *
     * This allows structurally identical logs to share
     * one fingerprint even when numbered identifiers vary.
     */
    private static final Pattern ALPHANUMERIC_SUFFIX_PATTERN =
            Pattern.compile(
                    "\\b([A-Za-z_-]+)\\d+\\b"
            );

    /*
     * Generic key=value token detector.
     *
     * Example:
     *
     * user=om
     * ip=10.0.0.1
     * result=deny
     */
    private static final Pattern KEY_VALUE_PATTERN =
            Pattern.compile(
                    "([A-Za-z_][A-Za-z0-9_.-]*)" +
                    "\\s*([=:])\\s*" +
                    "(\"[^\"]*\"|'[^']*'|[^\\s|;,]+)"
            );

    /*
     * =========================================================
     * PUBLIC API
     * =========================================================
     */

    public StructureFingerprint analyze(
            String rawLog
    ) {

        if (rawLog == null
                || rawLog.isBlank()) {

            throw new IllegalArgumentException(
                    "Raw log cannot be empty"
            );
        }

        String trimmed =
                rawLog.trim();

        String delimiter =
                detectDelimiter(
                        trimmed
                );

        String separator =
                detectKeyValueSeparator(
                        trimmed
                );

        String signature =
                detectSignaturePrefix(
                        trimmed
                );

        String canonical =
                canonicalize(
                        trimmed
                );

        String fingerprint =
                sha256(
                        canonical
                );

        return new StructureFingerprint(
                fingerprint,
                signature,
                delimiter,
                separator,
                canonical
        );
    }

    /*
     * =========================================================
     * CANONICALIZATION
     * =========================================================
     *
     * Convert changing values into stable placeholders.
     *
     * Example:
     *
     * RAVEN#91 node=delta9 account=root ip=172.16.7.12
     *
     * becomes something like:
     *
     * raven#<num>
     * node=<value>
     * account=<value>
     * ip=<ip>
     */

    private String canonicalize(
            String raw
    ) {

        String value =
                raw
                        .replace("\r", " ")
                        .replace("\n", " ")
                        .trim();

        /*
         * Normalize timestamps first.
         */
        value =
                ISO_TIMESTAMP_PATTERN
                        .matcher(value)
                        .replaceAll("<timestamp>");

        value =
                SYSLOG_TIMESTAMP_PATTERN
                        .matcher(value)
                        .replaceAll("<timestamp>");

        /*
         * Normalize UUIDs.
         */
        value =
                UUID_PATTERN
                        .matcher(value)
                        .replaceAll("<uuid>");

        /*
         * Normalize IPs.
         */
        value =
                IPV4_PATTERN
                        .matcher(value)
                        .replaceAll("<ip>");

        /*
         * Normalize hex values.
         */
        value =
                HEX_PATTERN
                        .matcher(value)
                        .replaceAll("<hex>");

        /*
         * Normalize key/value pairs.
         *
         * Keep the KEY because it describes structure.
         * Replace only the changing VALUE.
         */
        Matcher matcher =
                KEY_VALUE_PATTERN
                        .matcher(value);

        StringBuffer buffer =
                new StringBuffer();

        while (matcher.find()) {

            String key =
                    matcher.group(1)
                            .toLowerCase(
                                    Locale.ROOT
                            );

            String separator =
                    matcher.group(2);

            String originalValue =
                    matcher.group(3);

            String placeholder =
                    classifyValue(
                            originalValue
                    );

            String replacement =
                    key
                            + separator
                            + placeholder;

            matcher.appendReplacement(
                    buffer,
                    Matcher.quoteReplacement(
                            replacement
                    )
            );
        }

        matcher.appendTail(
                buffer
        );

        value =
                buffer.toString();

        /*
         * Normalize changing numeric suffixes attached to words.
         *
         * Examples:
         *
         * node1  -> node<num>
         * node2  -> node<num>
         * user15 -> user<num>
         *
         * Without this step each of those values would create
         * a different structural fingerprint.
         */
        value =
                ALPHANUMERIC_SUFFIX_PATTERN
                        .matcher(value)
                        .replaceAll("$1<num>");

        /*
         * Remaining standalone numbers.
         *
         * Example:
         *
         * RAVEN#1 -> RAVEN#<num>
         */
        value =
                NUMBER_PATTERN
                        .matcher(value)
                        .replaceAll("<num>");

        /*
         * Normalize whitespace.
         */
        value =
                value
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return value;
    }

    private String classifyValue(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "<empty>";
        }

        String cleaned =
                stripQuotes(
                        value.trim()
                );

        if (IPV4_PATTERN
                .matcher(cleaned)
                .matches()) {

            return "<ip>";
        }

        if (UUID_PATTERN
                .matcher(cleaned)
                .matches()) {

            return "<uuid>";
        }

        if (ISO_TIMESTAMP_PATTERN
                .matcher(cleaned)
                .matches()) {

            return "<timestamp>";
        }

        if (cleaned.matches(
                "\\d+"
        )) {

            return "<num>";
        }

        if (cleaned.matches(
                "0x[0-9a-fA-F]+"
        )) {

            return "<hex>";
        }

        /*
         * Preserve semantic constants that help
         * distinguish event families.
         *
         * Examples:
         *
         * deny
         * blocked
         * success
         *
         * These values may define the event structure
         * rather than merely being random data.
         */
        String lower =
                cleaned.toLowerCase(
                        Locale.ROOT
                );

        if (isSemanticConstant(
                lower
        )) {

            return lower;
        }

        return "<value>";
    }

    private boolean isSemanticConstant(
            String value
    ) {

        return switch (value) {

            case
                "allow",
                "allowed",
                "accept",
                "accepted",
                "success",
                "successful",
                "deny",
                "denied",
                "block",
                "blocked",
                "reject",
                "rejected",
                "fail",
                "failed",
                "error",
                "warning",
                "critical",
                "info",
                "tcp",
                "udp",
                "icmp",
                "ssh",
                "http",
                "https",
                "dns"
                    -> true;

            default
                    -> false;
        };
    }

    /*
     * =========================================================
     * SIGNATURE DETECTION
     * =========================================================
     */

    private String detectSignaturePrefix(
            String raw
    ) {

        String first =
                firstLine(
                        raw
                );

        if (first.startsWith("CEF:")) {
            return "CEF:";
        }

        if (first.startsWith("LEEF:")) {
            return "LEEF:";
        }

        if (first.startsWith("{")) {
            return "JSON";
        }

        /*
         * Capture only the stable structural prefix
         * before the first major delimiter.
         *
         * Examples:
         *
         * RAVEN#1~node1~user1...
         * -> RAVEN#
         *
         * MYSTERY_EVT@@host=...
         * -> MYSTERY_EVT
         *
         * NEBULA::user=...
         * -> NEBULA
         */
        String token =
                first.split(
                        "(?:@@|::|~|\\||\\s)",
                        2
                )[0];

        /*
         * Remove trailing variable numbers.
         *
         * RAVEN#1 -> RAVEN#
         * DEVICE42 -> DEVICE
         */
        token =
                token.replaceAll(
                        "\\d+$",
                        ""
                );

        if (token.length() > 32) {

            token =
                    token.substring(
                            0,
                            32
                    );
        }

        if (!token.isBlank()) {

            return token;
        }

        return "UNKNOWN";
    }

    /*
     * =========================================================
     * DELIMITER DETECTION
     * =========================================================
     */

    private String detectDelimiter(
            String raw
    ) {

        if (raw.contains("@@")) {
            return "@@";
        }

        if (raw.contains("::")) {
            return "::";
        }

        if (raw.contains("||")) {
            return "||";
        }

        if (raw.contains("~")) {
            return "~";
        }

        if (raw.contains("\t")) {
            return "\t";
        }

        if (raw.contains("|")) {
            return "|";
        }

        if (raw.contains(";")) {
            return ";";
        }

        if (raw.contains(",")) {
            return ",";
        }

        return " ";
    }

    private String detectKeyValueSeparator(
            String raw
    ) {

        if (raw.contains("==")) {
            return "==";
        }

        if (raw.contains("=")) {
            return "=";
        }

        /*
         * Avoid treating timestamps such as
         * 21:30:01 as generic KV syntax.
         */
        if (
                raw.matches(
                        ".*\\b[A-Za-z_][A-Za-z0-9_.-]*:[^\\s]+.*"
                )
        ) {

            return ":";
        }

        if (raw.contains("->")) {
            return "->";
        }

        return "=";
    }

    /*
     * =========================================================
     * HASHING
     * =========================================================
     */

    private String sha256(
            String value
    ) {

        try {

            MessageDigest digest =
                    MessageDigest
                            .getInstance(
                                    "SHA-256"
                            );

            byte[] hash =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(
                            hash
                    );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to generate structure fingerprint",
                    e
            );
        }
    }

    private String stripQuotes(
            String value
    ) {

        if (value == null
                || value.length() < 2) {

            return value;
        }

        if (
                (
                    value.startsWith("\"")
                    &&
                    value.endsWith("\"")
                )
                ||
                (
                    value.startsWith("'")
                    &&
                    value.endsWith("'")
                )
        ) {

            return value.substring(
                    1,
                    value.length() - 1
            );
        }

        return value;
    }

    private String firstLine(
            String raw
    ) {

        int index =
                raw.indexOf('\n');

        if (index < 0) {
            return raw.trim();
        }

        return raw.substring(
                0,
                index
        ).trim();
    }

    /*
     * =========================================================
     * RESULT
     * =========================================================
     */

    public record StructureFingerprint(

            String fingerprint,

            String signaturePrefix,

            String delimiter,

            String keyValueSeparator,

            String canonicalStructure

    ) {
    }
}