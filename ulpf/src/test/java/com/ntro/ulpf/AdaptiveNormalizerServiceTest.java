package com.ntro.ulpf;

import com.ntro.ulpf.service.LogStructureFingerprintService;
import com.ntro.ulpf.service.NormalizationConfidenceService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveNormalizerServiceTest {

    @Test
    void autoApprovalThresholdIsConfigurable() {

        NormalizationConfidenceService service =
                new NormalizationConfidenceService(
                        true,
                        0.95,
                        true
                );

        assertTrue(
                service.shouldAutoApprove(
                        0.96
                )
        );

        assertFalse(
                service.shouldAutoApprove(
                        0.90
                )
        );

        assertTrue(
                service.getAutoApproveThreshold()
                        > 0.0
        );
    }

    @Test
    void structurallySimilarLogsProduceSameFingerprint() {

        LogStructureFingerprintService service =
                new LogStructureFingerprintService();

        String first =
                "RAVEN#1 node=node1 "
                        + "account=user1 "
                        + "ip=10.10.1.1 "
                        + "result=blocked";

        String second =
                "RAVEN#2 node=node2 "
                        + "account=user2 "
                        + "ip=10.10.1.2 "
                        + "result=blocked";

        var firstFingerprint =
                service.analyze(
                        first
                );

        var secondFingerprint =
                service.analyze(
                        second
                );

        assertEquals(
                firstFingerprint.fingerprint(),
                secondFingerprint.fingerprint()
        );

        assertEquals(
                firstFingerprint.canonicalStructure(),
                secondFingerprint.canonicalStructure()
        );
    }

    @Test
    void structurallyDifferentLogsProduceDifferentFingerprints() {

        LogStructureFingerprintService service =
                new LogStructureFingerprintService();

        String raven =
                "RAVEN#1 node=node1 "
                        + "account=user1 "
                        + "ip=10.10.1.1 "
                        + "result=blocked";

        String nebula =
                "NEBULA::identity=om"
                        + "::device=firewall"
                        + "::src=10.10.1.1"
                        + "::decision=deny";

        var ravenFingerprint =
                service.analyze(
                        raven
                );

        var nebulaFingerprint =
                service.analyze(
                        nebula
                );

        assertNotEquals(
                ravenFingerprint.fingerprint(),
                nebulaFingerprint.fingerprint()
        );
    }

    @Test
    void fingerprintDetectsReusableStructureMetadata() {

        LogStructureFingerprintService service =
                new LogStructureFingerprintService();

        String raw =
                "MYSTERY_EVT@@host=beta-seven"
                        + "@@usr=om"
                        + "@@addr=192.168.77.21"
                        + "@@auth=no";

        var result =
                service.analyze(
                        raw
                );

        assertEquals(
                "@@",
                result.delimiter()
        );

        assertEquals(
                "=",
                result.keyValueSeparator()
        );

        assertFalse(
                result.fingerprint()
                        .isBlank()
        );

        assertFalse(
                result.canonicalStructure()
                        .isBlank()
        );
    }
}