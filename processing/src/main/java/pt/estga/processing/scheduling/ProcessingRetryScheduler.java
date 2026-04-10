package pt.estga.processing.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.services.AsyncProcessingService;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingRetryScheduler {

    private final MarkEvidenceProcessingRepository processingRepository;
    private final AsyncProcessingService asyncProcessingService;
    private final MeterRegistry meterRegistry;

    /**
     * Periodically retry PENDING and FAILED processing entries.
     * Interval is configurable via property 'processing.retry.interval' (milliseconds).
     */
    @Scheduled(fixedDelayString = "${processing.retry.interval:60000}")
    public void retryPending() {
        try {
            var pending = processingRepository.findByStatusIn(List.of(ProcessingStatus.PENDING, ProcessingStatus.FAILED));
            if (pending == null || pending.isEmpty()) {
                return;
            }
            log.info("Retrying {} pending/failed processing entries", pending.size());
            meterRegistry.counter("processing.retry.invocations").increment();
            for (var p : pending) {
                Long submissionId = p.getSubmission() != null ? p.getSubmission().getId() : null;
                if (submissionId == null) {
                    log.warn("Skipping processing entry {} with no submission linked", p.getId());
                    continue;
                }
                meterRegistry.counter("processing.retry.scheduled").increment();
                asyncProcessingService.processAsync(submissionId);
            }
        } catch (Exception e) {
            log.error("Error while retrying pending processing entries: {}", e.getMessage(), e);
        }
    }
}
