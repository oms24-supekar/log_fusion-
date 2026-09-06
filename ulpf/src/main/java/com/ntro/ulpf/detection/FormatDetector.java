package com.ntro.ulpf.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class FormatDetector {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern SYSLOG_PATTERN = Pattern.compile(
            "^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+.+"
    );

  private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(
    "(?:^|[\\s,])[^\\s=,]+=[^\\s,]+"
);

    public LogFormat detect(String rawLog) {

        if (rawLog == null || rawLog.isBlank()) {
            return LogFormat.UNKNOWN;
        }

        String log = rawLog.trim();

        // 1. CEF
        if (log.startsWith("CEF:")) {
            return LogFormat.CEF;
        }

        // 2. JSON
        if (isJson(log)) {
            return LogFormat.JSON;
        }

        // 3. SYSLOG
        if (SYSLOG_PATTERN.matcher(log).matches()) {
            return LogFormat.SYSLOG;
        }

        // 4. KEY-VALUE
        if (hasMultipleKeyValuePairs(log)) {
            return LogFormat.KEY_VALUE;
        }

        // 5. UNKNOWN
        return LogFormat.UNKNOWN;
    }

    private boolean isJson(String log) {

        if (!(log.startsWith("{") && log.endsWith("}"))) {
            return false;
        }

        try {
            objectMapper.readTree(log);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasMultipleKeyValuePairs(String log) {

        var matcher = KEY_VALUE_PATTERN.matcher(log);

        int count = 0;

        while (matcher.find()) {
            count++;

            if (count >= 2) {
                return true;
            }
        }

        return false;
    }
}