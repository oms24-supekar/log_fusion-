package com.ntro.ulpf.dto;

public record UnknownLogApprovalResponse(
        ParserDefinitionResponse parserDefinition,
        LogResponse reprocessedLog
) {
}