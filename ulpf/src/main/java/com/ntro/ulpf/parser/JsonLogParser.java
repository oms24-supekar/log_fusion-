package com.ntro.ulpf.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntro.ulpf.detection.LogFormat;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonLogParser implements LogParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(LogFormat format) {
        return format == LogFormat.JSON;
    }

    @Override
    public ParsedLog parse(String rawLog) {

        try {

            Map<String, Object> fields =
                    objectMapper.readValue(
                            rawLog,
                            new TypeReference<Map<String, Object>>() {
                            }
                    );

            return new ParsedLog(
                    LogFormat.JSON,
                    fields
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Unable to parse JSON log",
                    e
            );
        }
    }
}