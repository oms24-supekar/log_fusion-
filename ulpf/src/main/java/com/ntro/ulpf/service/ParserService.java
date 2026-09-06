package com.ntro.ulpf.service;

import com.ntro.ulpf.detection.FormatDetector;
import com.ntro.ulpf.detection.LogFormat;
import com.ntro.ulpf.dto.ParserTestResponse;
import com.ntro.ulpf.parser.LogParser;
import com.ntro.ulpf.parser.ParsedLog;
import com.ntro.ulpf.parser.ParserRegistry;
import org.springframework.stereotype.Service;

@Service
public class ParserService {

    private final FormatDetector formatDetector;
    private final ParserRegistry parserRegistry;

    public ParserService(
            FormatDetector formatDetector,
            ParserRegistry parserRegistry
    ) {
        this.formatDetector = formatDetector;
        this.parserRegistry = parserRegistry;
    }

    public ParserTestResponse parse(String rawLog) {

        LogFormat format =
                formatDetector.detect(rawLog);

        if (format == LogFormat.UNKNOWN) {
            throw new IllegalArgumentException(
                    "Unknown log format cannot be parsed automatically"
            );
        }

        LogParser parser =
                parserRegistry.getParser(format);

        ParsedLog parsedLog =
                parser.parse(rawLog);

        return new ParserTestResponse(
                parsedLog.format().name(),
                parsedLog.fields()
        );
    }
}