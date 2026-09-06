package com.ntro.ulpf.parser;

import com.ntro.ulpf.detection.LogFormat;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SyslogParser implements LogParser {

    private static final Pattern SYSLOG_PATTERN = Pattern.compile(
            "^(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+" +
            "(\\S+)\\s+" +
            "([^\\[:]+)" +
            "(?:\\[(\\d+)\\])?:\\s*" +
            "(.*)$"
    );

    private static final Pattern FAILED_PASSWORD_PATTERN = Pattern.compile(
            "Failed password for (?:invalid user )?(\\S+) from ([0-9a-fA-F:.]+)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean supports(LogFormat format) {
        return format == LogFormat.SYSLOG;
    }

    @Override
    public ParsedLog parse(String rawLog) {

        Matcher matcher = SYSLOG_PATTERN.matcher(rawLog.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Unable to parse SYSLOG log"
            );
        }

        Map<String, Object> fields = new LinkedHashMap<>();

        fields.put("timestamp", matcher.group(1));
        fields.put("host", matcher.group(2));
        fields.put("process", matcher.group(3));

        if (matcher.group(4) != null) {
            fields.put("pid", matcher.group(4));
        }

        String message = matcher.group(5);

        fields.put("message", message);

        Matcher authMatcher =
                FAILED_PASSWORD_PATTERN.matcher(message);

        if (authMatcher.find()) {
            fields.put("username", authMatcher.group(1));
            fields.put("source_ip", authMatcher.group(2));
            fields.put("action", "login_failed");
        }

        return new ParsedLog(
                LogFormat.SYSLOG,
                fields
        );
    }
}