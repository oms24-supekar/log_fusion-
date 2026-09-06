package com.ntro.ulpf.controller;

import com.ntro.ulpf.dto.CreateParserDefinitionRequest;
import com.ntro.ulpf.dto.DynamicParserTestResponse;
import com.ntro.ulpf.dto.ParserDefinitionResponse;
import com.ntro.ulpf.service.DynamicParserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/parser-definitions")
public class ParserDefinitionController {

    private final DynamicParserService dynamicParserService;

    public ParserDefinitionController(
            DynamicParserService dynamicParserService
    ) {
        this.dynamicParserService =
                dynamicParserService;
    }

    @PostMapping
    public ResponseEntity<ParserDefinitionResponse>
    createDefinition(
            @Valid
            @RequestBody
            CreateParserDefinitionRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        dynamicParserService
                                .createDefinition(request)
                );
    }

    @GetMapping
    public ResponseEntity<List<ParserDefinitionResponse>>
    getAllDefinitions() {

        return ResponseEntity.ok(
                dynamicParserService
                        .getAllDefinitions()
        );
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<DynamicParserTestResponse>
    testDefinition(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request
    ) {

        String rawLog =
                request.get("rawContent");

        if (rawLog == null
                || rawLog.isBlank()) {

            throw new IllegalArgumentException(
                    "rawContent is required"
            );
        }

        return ResponseEntity.ok(
                dynamicParserService
                        .testDefinition(
                                id,
                                rawLog
                        )
        );
    }
}