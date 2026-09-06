package com.ntro.ulpf.parser;

import com.ntro.ulpf.detection.LogFormat;

public interface LogParser {

    boolean supports(LogFormat format);

    ParsedLog parse(String rawLog);
}