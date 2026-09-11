package com.ntro.ulpf;

import com.ntro.ulpf.detection.FormatDetector;
import com.ntro.ulpf.detection.LogFormat;
import com.ntro.ulpf.dto.LogRequest;
import com.ntro.ulpf.repository.NormalizedLogRepository;
import com.ntro.ulpf.repository.RawLogRepository;
import com.ntro.ulpf.service.AutomaticNormalizationService;
import com.ntro.ulpf.service.DynamicParserService;
import com.ntro.ulpf.service.RawLogService;
import com.ntro.ulpf.normalization.NormalizationEngine;
import com.ntro.ulpf.parser.ParserRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogRequestBackdatingTest {

    @Test
    void saveRawLogUsesProvidedReceivedAtWhenPresent() {
        RawLogRepository rawLogRepository = mock(RawLogRepository.class);
        NormalizedLogRepository normalizedLogRepository = mock(NormalizedLogRepository.class);
        FormatDetector formatDetector = mock(FormatDetector.class);
        ParserRegistry parserRegistry = mock(ParserRegistry.class);
        NormalizationEngine normalizationEngine = mock(NormalizationEngine.class);
        DynamicParserService dynamicParserService = mock(DynamicParserService.class);
        AutomaticNormalizationService automaticNormalizationService = mock(AutomaticNormalizationService.class);

        RawLogService service = new RawLogService(
                rawLogRepository,
                normalizedLogRepository,
                formatDetector,
                parserRegistry,
                normalizationEngine,
                dynamicParserService,
                automaticNormalizationService
        );

        LocalDateTime historicalDate = LocalDateTime.now().minusDays(75);
        when(formatDetector.detect(anyString())).thenReturn(LogFormat.UNKNOWN);
        when(rawLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(dynamicParserService.tryParse(anyString())).thenReturn(Optional.empty());

        service.saveRawLog(new LogRequest(
                "timestamp=2026-09-11T18:50:01+05:30 level=INFO source=firewall-01 action=ALLOW src_ip=192.168.1.20 dst_ip=8.8.4.4 protocol=UDP dst_port=53",
                "firewall-01",
                "network",
                historicalDate
        ));

        ArgumentCaptor<com.ntro.ulpf.entity.RawLog> captor = ArgumentCaptor.forClass(com.ntro.ulpf.entity.RawLog.class);
        verify(rawLogRepository).save(captor.capture());
        assertEquals(historicalDate, captor.getValue().getReceivedAt());
    }
}
