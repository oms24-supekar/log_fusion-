package com.ntro.ulpf.service;

import com.ntro.ulpf.dto.BatchAcceptedResponse;
import com.ntro.ulpf.entity.BatchJob;
import com.ntro.ulpf.repository.BatchJobRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BatchIngestionService {

    private final BatchJobRepository batchJobRepository;

    private final Path storageDirectory;

    public BatchIngestionService(
            BatchJobRepository batchJobRepository,
            @Value(
                "${logfusion.batch.storage-dir:./data/batches}"
            )
            String storageDirectory
    ) {

        this.batchJobRepository =
                batchJobRepository;

        this.storageDirectory =
                Path.of(storageDirectory)
                        .toAbsolutePath()
                        .normalize();
    }

    public BatchAcceptedResponse accept(
            MultipartFile file,
            String sourceName,
            String sourceType
    ) {

        if (file == null
                || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Batch file cannot be empty"
            );
        }

        try {

            Files.createDirectories(
                    storageDirectory
            );

            UUID batchId =
                    UUID.randomUUID();

            LocalDateTime now =
                    LocalDateTime.now();

            String originalFileName =
                    sanitizeFileName(
                            file.getOriginalFilename()
                    );

            String storedFileName =
                    batchId
                            + "-"
                            + originalFileName;

            Path targetPath =
                    storageDirectory
                            .resolve(
                                    storedFileName
                            )
                            .normalize();

            /*
             * Prevent path traversal.
             */
            if (!targetPath.startsWith(
                    storageDirectory
            )) {

                throw new IllegalArgumentException(
                        "Invalid file path"
                );
            }

            /*
             * STREAM upload to disk.
             *
             * We do NOT use:
             * readAllBytes()
             * Files.readAllLines()
             *
             * Therefore a 100k+ line file does not
             * have to live entirely in JVM memory.
             */
            try (
                    InputStream inputStream =
                            file.getInputStream()
            ) {

                Files.copy(
                        inputStream,
                        targetPath,
                        StandardCopyOption
                                .REPLACE_EXISTING
                );
            }

            BatchJob batchJob =
                    new BatchJob(
                            batchId,
                            originalFileName,
                            targetPath.toString(),
                            normalizeSourceName(
                                    sourceName
                            ),
                            normalizeSourceType(
                                    sourceType
                            ),
                            "ACCEPTED",

                            /*
                             * Counts are populated
                             * by the batch processor
                             * in Chunk 3.
                             */
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,

                            now,
                            null,
                            now
                    );

            batchJobRepository.save(
                    batchJob
            );

            return new BatchAcceptedResponse(
                    batchId,
                    originalFileName,
                    "ACCEPTED",
                    now
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to accept batch upload: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private String sanitizeFileName(
            String fileName
    ) {

        if (fileName == null
                || fileName.isBlank()) {

            return "batch.log";
        }

        String safeName =
                Path.of(fileName)
                        .getFileName()
                        .toString();

        safeName =
                safeName.replaceAll(
                        "[^a-zA-Z0-9._-]",
                        "_"
                );

        if (safeName.isBlank()) {
            return "batch.log";
        }

        return safeName;
    }

    private String normalizeSourceName(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "batch-upload";
        }

        return value.trim();
    }

    private String normalizeSourceType(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "UNKNOWN";
        }

        return value.trim();
    }
}