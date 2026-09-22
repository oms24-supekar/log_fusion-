package com.ntro.ulpf.dto;

import java.util.List;
import java.util.UUID;

public record BatchLogsPageResponse(
        UUID batchId,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        List<BatchLogResponse> logs
) {
}
