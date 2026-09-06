package com.ntro.ulpf.parser;

import com.ntro.ulpf.detection.LogFormat;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CefParser implements LogParser {

    @Override
    public boolean supports(LogFormat format) {
        return format == LogFormat.CEF;
    }

    @Override
    public ParsedLog parse(String rawLog) {

        if (rawLog == null || !rawLog.startsWith("CEF:")) {
            throw new IllegalArgumentException(
                    "Invalid CEF log"
            );
        }

        String[] parts = rawLog.split("\\|", 8);

        if (parts.length < 7) {
            throw new IllegalArgumentException(
                    "Unable to parse CEF log"
            );
        }

        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "cef_version",
                parts[0].substring(4)
        );

        fields.put("vendor", parts[1]);
        fields.put("product", parts[2]);
        fields.put("version", parts[3]);
        fields.put("signature_id", parts[4]);
        fields.put("name", parts[5]);
        fields.put("severity", parts[6]);

        if (parts.length == 8) {

            String extension = parts[7];

            String[] pairs = extension.split("\\s+");

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
        }

        return new ParsedLog(
                LogFormat.CEF,
                fields
        );
    }
}