package com.ntro.ulpf.controller;

import com.ntro.ulpf.dto.BatchLogsPageResponse;
import com.ntro.ulpf.dto.BatchProgressResponse;
import com.ntro.ulpf.service.BatchQueryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/batches")
public class BatchQueryController {

    private final BatchQueryService batchQueryService;

    public BatchQueryController(
            BatchQueryService batchQueryService
    ) {
        this.batchQueryService =
                batchQueryService;
    }

    @GetMapping
    public ResponseEntity<List<BatchProgressResponse>>
    getAllBatches() {

        return ResponseEntity.ok(
                batchQueryService.getAllBatches()
        );
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<BatchProgressResponse>
    getBatch(
            @PathVariable UUID batchId
    ) {

        return ResponseEntity.ok(
                batchQueryService.getBatch(
                        batchId
                )
        );
    }

    @GetMapping("/{batchId}/logs")
    public ResponseEntity<BatchLogsPageResponse>
    getBatchLogs(
            @PathVariable UUID batchId,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "100"
            )
            int size
    ) {

        return ResponseEntity.ok(
                batchQueryService.getBatchLogs(
                        batchId,
                        page,
                        size
                )
        );
    }
}
