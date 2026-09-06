package com.ntro.ulpf.parser;

import com.ntro.ulpf.entity.ParserDefinition;

public record DynamicParseResult(
        ParserDefinition definition,
        ParsedLog parsedLog
) {
}