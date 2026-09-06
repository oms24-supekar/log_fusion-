package com.ntro.ulpf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntro.ulpf.dto.LogDetailsResponse;
import com.ntro.ulpf.dto.LogSummaryResponse;
import com.ntro.ulpf.dto.NormalizedLogDetailsResponse;
import com.ntro.ulpf.dto.RawLogDetailsResponse;
import com.ntro.ulpf.entity.NormalizedLog;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.repository.NormalizedLogRepository;
import com.ntro.ulpf.repository.RawLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LogQueryService {

    private final RawLogRepository rawLogRepository;
    private final NormalizedLogRepository normalizedLogRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    public LogQueryService(
            RawLogRepository rawLogRepository,
            NormalizedLogRepository normalizedLogRepository
    ) {
        this.rawLogRepository = rawLogRepository;
        this.normalizedLogRepository = normalizedLogRepository;
    }

    public List<LogSummaryResponse> getAllLogs() {

        return rawLogRepository
                .findAllByOrderByReceivedAtDesc()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public List<LogSummaryResponse> getUnknownLogs() {

        return rawLogRepository
                .findByProcessingStatusOrderByReceivedAtDesc("NEEDS_REVIEW")
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public LogDetailsResponse getLogById(UUID id) {

        RawLog rawLog = rawLogRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Log not found with id: " + id
                        )
                );

        RawLogDetailsResponse rawResponse =
                new RawLogDetailsResponse(
                        rawLog.getId(),
                        rawLog.getRawContent(),
                        rawLog.getSourceName(),
                        rawLog.getSourceType(),
                        rawLog.getDetectedFormat(),
                        rawLog.getSha256Hash(),
                        rawLog.getProcessingStatus(),
                        rawLog.getReceivedAt()
                );

        NormalizedLogDetailsResponse normalizedResponse =
                normalizedLogRepository
                        .findByRawLog_Id(id)
                        .map(this::toNormalizedResponse)
                        .orElse(null);

        return new LogDetailsResponse(
                rawResponse,
                normalizedResponse
        );
    }

    private LogSummaryResponse toSummaryResponse(RawLog rawLog) {

        return new LogSummaryResponse(
                rawLog.getId(),
                rawLog.getSourceName(),
                rawLog.getSourceType(),
                rawLog.getDetectedFormat(),
                rawLog.getProcessingStatus(),
                rawLog.getSha256Hash(),
                rawLog.getReceivedAt()
        );
    }

    private NormalizedLogDetailsResponse toNormalizedResponse(
            NormalizedLog normalizedLog
    ) {

        try {

            JsonNode normalizedJson =
                    objectMapper.readTree(
                            normalizedLog.getNormalizedData()
                    );

            return new NormalizedLogDetailsResponse(
                    normalizedLog.getId(),
                    normalizedJson,
                    normalizedLog.getParserUsed(),
                    normalizedLog.getValidationStatus(),
                    normalizedLog.getProcessedAt()
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to read normalized JSON for log: "
                            + normalizedLog.getId(),
                    e
            );
        }
    }
}