package com.ntro.ulpf.controller;

import com.ntro.ulpf.dto.LogDetailsResponse;
import com.ntro.ulpf.dto.LogRequest;
import com.ntro.ulpf.dto.LogResponse;
import com.ntro.ulpf.dto.LogSummaryResponse;
import com.ntro.ulpf.service.LogQueryService;
import com.ntro.ulpf.service.RawLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final RawLogService rawLogService;
    private final LogQueryService logQueryService;

    public LogController(
            RawLogService rawLogService,
            LogQueryService logQueryService
    ) {
        this.rawLogService = rawLogService;
        this.logQueryService = logQueryService;
    }

    @PostMapping("/process")
    public ResponseEntity<LogResponse> processLog(
            @Valid @RequestBody LogRequest request
    ) {

        LogResponse response =
                rawLogService.saveRawLog(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<LogSummaryResponse>> getAllLogs() {

        return ResponseEntity.ok(
                logQueryService.getAllLogs()
        );
    }

    @GetMapping("/unknown")
    public ResponseEntity<List<LogSummaryResponse>> getUnknownLogs() {

        return ResponseEntity.ok(
                logQueryService.getUnknownLogs()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogDetailsResponse> getLogById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                logQueryService.getLogById(id)
        );
    }
}