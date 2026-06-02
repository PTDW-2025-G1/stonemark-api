package pt.estga.processing.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingStuckReaper {

    private final MarkEvidenceProcessingRepository processingRepository;
    private final MeterRegistry meterRegistry;

    @Value("${processing.reaper.max-stuck-minutes:30}")
    private long maxStuckMinutes;

    @Scheduled(fixedDelayString = "${processing.reaper.interval:3600000}")
    @Transactional
    public void reapStuckProcessing() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(maxStuckMinutes));
        Instant now = Instant.now();

        int reset = processingRepository.resetStuckProcessing(ProcessingStatus.PROCESSING, ProcessingStatus.PENDING, cutoff, now);
        if (reset > 0) {
            log.warn("Reaper reset {} stuck PROCESSING records to PENDING (cutoff={})", reset, cutoff);
            meterRegistry.counter("processing.reaper.reset").increment(reset);
        }
    }
}
