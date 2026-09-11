package com.ntro.ulpf.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NormalizationConfidenceService {

    private final boolean autoApproveEnabled;
    private final double autoApproveThreshold;
    private final boolean aiEnabled;

    public NormalizationConfidenceService(
            @Value("${normalizer.auto-approve-enabled:true}") boolean autoApproveEnabled,
            @Value("${normalizer.auto-approve-threshold:0.95}") double autoApproveThreshold,
            @Value("${normalizer.ai-enabled:true}") boolean aiEnabled
    ) {
        this.autoApproveEnabled = autoApproveEnabled;
        this.autoApproveThreshold = autoApproveThreshold;
        this.aiEnabled = aiEnabled;
    }

    public double calculate(
            double structureConfidence,
            double mappingConfidence,
            int recognizedFields,
            int conflictCount,
            boolean validationPassed
    ) {
        double score = (structureConfidence * 0.35)
                + (mappingConfidence * 0.45)
                + (Math.min(recognizedFields, 15) / 15.0 * 0.10)
                + (validationPassed ? 0.10 : 0.00)
                - (conflictCount * 0.05);

        return Math.max(0.0, Math.min(1.0, score));
    }

    public boolean shouldAutoApprove(double confidenceScore) {
        return autoApproveEnabled && confidenceScore >= autoApproveThreshold;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public double getAutoApproveThreshold() {
        return autoApproveThreshold;
    }
}
