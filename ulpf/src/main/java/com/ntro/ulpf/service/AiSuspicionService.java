package com.ntro.ulpf.service;

import com.ntro.ulpf.dto.AiReviewMetadata;
import com.ntro.ulpf.dto.FieldReview;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiSuspicionService {

    public AiReviewMetadata review(
            String rawLog,
            Map<String, Object> aiFields
    ) {

        List<FieldReview> reviews =
                new ArrayList<>();

        if (aiFields == null || aiFields.isEmpty()) {
            return new AiReviewMetadata(
                    0.0,
                    reviews
            );
        }

        double totalConfidence = 0.0;
        int countedFields = 0;

        for (Map.Entry<String, Object> entry
                : aiFields.entrySet()) {

            String field =
                    entry.getKey();

            Object value =
                    entry.getValue();

            if (value == null) {
                continue;
            }

            String textValue =
                    String.valueOf(value).trim();

            if (textValue.isBlank()) {
                continue;
            }

            double confidence;
            boolean suspicious;
            String reason;

            /*
             * Exact value appears in original raw log.
             */
            if (containsIgnoreCase(
                    rawLog,
                    textValue
            )) {

                confidence = 0.98;
                suspicious = false;

                reason =
                        "Exact value found in raw log";

            } else if (isCommonInferenceField(
                    field
            )) {

                /*
                 * These fields are commonly inferred
                 * from semantic meaning instead of
                 * copied directly.
                 */
                confidence = 0.75;
                suspicious = true;

                reason =
                        "Value inferred from log meaning";

            } else {

                /*
                 * AI returned a value that does not
                 * visibly appear in the source log.
                 */
                confidence = 0.55;
                suspicious = true;

                reason =
                        "Value not directly present in raw log";
            }

            reviews.add(
                    new FieldReview(
                            field,
                            confidence,
                            suspicious,
                            reason
                    )
            );

            totalConfidence +=
                    confidence;

            countedFields++;
        }

        double overallConfidence =
                countedFields == 0
                        ? 0.0
                        : totalConfidence
                        / countedFields;

        return new AiReviewMetadata(
                overallConfidence,
                reviews
        );
    }

    private boolean containsIgnoreCase(
            String rawLog,
            String value
    ) {

        if (rawLog == null
                || value == null) {

            return false;
        }

        return rawLog
                .toLowerCase()
                .contains(
                        value.toLowerCase()
                );
    }

    private boolean isCommonInferenceField(
            String field
    ) {

        if (field == null) {
            return false;
        }

        return switch (
                field.toLowerCase()
        ) {

            case "action",
                 "event_type",
                 "event",
                 "outcome",
                 "severity",
                 "category",
                 "source_type",
                 "service",
                 "protocol"
                    -> true;

            default
                    -> false;
        };
    }
}