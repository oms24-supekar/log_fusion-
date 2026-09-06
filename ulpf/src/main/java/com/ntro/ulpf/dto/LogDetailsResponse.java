package com.ntro.ulpf.dto;

public record LogDetailsResponse(
        RawLogDetailsResponse raw,
        NormalizedLogDetailsResponse normalized
) {
}