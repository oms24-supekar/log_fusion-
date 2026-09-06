package com.ntro.ulpf.parser;

import com.ntro.ulpf.detection.LogFormat;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParserRegistry {

    private final List<LogParser> parsers;

    public ParserRegistry(List<LogParser> parsers) {
        this.parsers = parsers;
    }

    public LogParser getParser(LogFormat format) {

        return parsers.stream()
                .filter(parser -> parser.supports(format))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No parser available for format: "
                                        + format
                        )
                );
    }
}