package com.ntro.ulpf.controller;

import com.ntro.ulpf.dto.BatchAcceptedResponse;
import com.ntro.ulpf.service.BatchIngestionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchIngestionService batchIngestionService;

    public BatchController(
            BatchIngestionService batchIngestionService
    ) {
        this.batchIngestionService =
                batchIngestionService;
    }

    @PostMapping(
            value = "/upload",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<BatchAcceptedResponse> upload(
            @RequestPart("file")
            MultipartFile file,

            @RequestParam(
                    value = "sourceName",
                    required = false
            )
            String sourceName,

            @RequestParam(
                    value = "sourceType",
                    required = false
            )
            String sourceType
    ) {

        BatchAcceptedResponse response =
                batchIngestionService.accept(
                        file,
                        sourceName,
                        sourceType
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }
}