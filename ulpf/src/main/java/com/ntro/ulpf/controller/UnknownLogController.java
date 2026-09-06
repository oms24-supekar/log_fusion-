package com.ntro.ulpf.controller;

import com.ntro.ulpf.dto.CreateParserDefinitionRequest;
import com.ntro.ulpf.dto.UnknownLogAnalysisResponse;
import com.ntro.ulpf.dto.UnknownLogApprovalResponse;
import com.ntro.ulpf.service.UnknownLogWorkflowService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/unknown-logs")
public class UnknownLogController {

    private final UnknownLogWorkflowService unknownLogWorkflowService;

    public UnknownLogController(
            UnknownLogWorkflowService unknownLogWorkflowService
    ) {
        this.unknownLogWorkflowService =
                unknownLogWorkflowService;
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<UnknownLogAnalysisResponse> analyze(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                unknownLogWorkflowService.analyze(id)
        );
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<UnknownLogApprovalResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody CreateParserDefinitionRequest request
    ) {
        return ResponseEntity.ok(
                unknownLogWorkflowService.approve(id, request)
        );
    }
}