package com.ntro.ulpf.service;

import com.ntro.ulpf.dto.CreateParserDefinitionRequest;
import com.ntro.ulpf.dto.LogResponse;
import com.ntro.ulpf.dto.ParserDefinitionResponse;
import com.ntro.ulpf.dto.UnknownLogAnalysisResponse;
import com.ntro.ulpf.dto.UnknownLogApprovalResponse;
import com.ntro.ulpf.entity.RawLog;
import com.ntro.ulpf.repository.RawLogRepository;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UnknownLogWorkflowService {

    private final RawLogRepository rawLogRepository;

    private final UnknownLogStructureAnalyzer
            unknownLogStructureAnalyzer;

    private final DynamicParserService
            dynamicParserService;

    private final RawLogService rawLogService;

    public UnknownLogWorkflowService(
            RawLogRepository rawLogRepository,
            UnknownLogStructureAnalyzer unknownLogStructureAnalyzer,
            DynamicParserService dynamicParserService,
            RawLogService rawLogService
    ) {
        this.rawLogRepository =
                rawLogRepository;

        this.unknownLogStructureAnalyzer =
                unknownLogStructureAnalyzer;

        this.dynamicParserService =
                dynamicParserService;

        this.rawLogService =
                rawLogService;
    }

    public UnknownLogAnalysisResponse analyze(
            UUID rawLogId
    ) {
        return unknownLogStructureAnalyzer
                .analyze(rawLogId);
    }

    public UnknownLogApprovalResponse approve(
            UUID rawLogId,
            CreateParserDefinitionRequest request
    ) {
        RawLog rawLog =
                rawLogRepository
                        .findById(rawLogId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Raw log not found: "
                                                + rawLogId
                                )
                        );

        if (!"NEEDS_REVIEW".equals(
                rawLog.getProcessingStatus()
        )) {
            throw new IllegalArgumentException(
                    "Only logs with NEEDS_REVIEW status can be approved"
            );
        }

        ParserDefinitionResponse definition =
                dynamicParserService
                        .createDefinition(request);

        LogResponse reprocessedLog =
                rawLogService
                        .reprocessDynamicLog(
                                rawLogId
                        );

        return new UnknownLogApprovalResponse(
                definition,
                reprocessedLog
        );
    }
}