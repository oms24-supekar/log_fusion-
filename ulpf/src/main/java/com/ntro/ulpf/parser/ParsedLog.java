package com.ntro.ulpf.parser;

import com.ntro.ulpf.detection.LogFormat;

import java.util.Map;

public record ParsedLog(
        LogFormat format,
        Map<String, Object> fields
) {
}