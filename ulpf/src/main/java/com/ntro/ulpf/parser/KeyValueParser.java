package com.ntro.ulpf.parser;

import com.ntro.ulpf.detection.LogFormat;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class KeyValueParser implements LogParser {

    @Override
    public boolean supports(LogFormat format) {
        return format == LogFormat.KEY_VALUE;
    }

    @Override
    public ParsedLog parse(String rawLog) {

        if (rawLog == null || rawLog.isBlank()) {
            throw new IllegalArgumentException(
                    "KEY_VALUE log cannot be empty"
            );
        }

        Map<String, Object> fields =
                new LinkedHashMap<>();

        String[] pairs = rawLog.trim().split("\\s+");

        for (String pair : pairs) {

            int equalsIndex = pair.indexOf('=');

            if (equalsIndex > 0) {

                String key =
                        pair.substring(0, equalsIndex);

                String value =
                        pair.substring(equalsIndex + 1);

                fields.put(key, value);
            }
        }

        if (fields.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to parse KEY_VALUE log"
            );
        }

        return new ParsedLog(
                LogFormat.KEY_VALUE,
                fields
        );
    }
}