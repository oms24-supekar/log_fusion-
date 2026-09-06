package com.ntro.ulpf.controller;

import com.ntro.ulpf.dto.ParserTestResponse;
import com.ntro.ulpf.service.ParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/parser")
public class ParserController {

    private final ParserService parserService;

    public ParserController(
            ParserService parserService
    ) {
        this.parserService = parserService;
    }

    @PostMapping("/test")
    public ResponseEntity<ParserTestResponse> testParser(
            @RequestBody Map<String, String> request
    ) {

        String rawLog =
                request.get("rawContent");

        if (rawLog == null || rawLog.isBlank()) {
            throw new IllegalArgumentException(
                    "rawContent is required"
            );
        }

        ParserTestResponse response =
                parserService.parse(rawLog);

        return ResponseEntity.ok(response);
    }
}