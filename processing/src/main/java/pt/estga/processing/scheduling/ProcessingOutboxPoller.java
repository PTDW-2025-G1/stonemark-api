package pt.estga.processing.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.ProcessingOutboxRepository;
import pt.estga.processing.services.processing.AsyncProcessingService;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingOutboxPoller {

    private final ProcessingOutboxRepository outboxRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final AsyncProcessingService asyncProcessingService;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${processing.outbox.poll-interval:5000}")
    public void poll() {
        try {
            var pageable = PageRequest.of(0, 20);
            var pending = outboxRepository.findByStatusOrderByCreatedAtAsc(ProcessingStatus.PENDING, pageable);
            if (pending == null || pending.isEmpty()) {
                return;
            }

            log.debug("Dispatching {} outbox entries", pending.size());
            for (var entry : pending) {
                if (processingRepository.findOverviewBySubmissionId(entry.getSubmissionId()).isPresent()) {
                    entry.setStatus(ProcessingStatus.DISPATCHED);
                    entry.setLastRetryAt(Instant.now());
                    outboxRepository.save(entry);
                    log.info("Outbox {} for submission {} skipped — processor already exists",
                            entry.getId(), entry.getSubmissionId());
                    meterRegistry.counter("processing.outbox.skipped", "reason", "already_exists").increment();
                    continue;
                }

                log.info("Outbox {} dispatching submission {} for processing",
                        entry.getId(), entry.getSubmissionId());
                meterRegistry.counter("processing.outbox.dispatched").increment();
                try {
                    asyncProcessingService.processAsync(entry.getSubmissionId());
                    entry.setStatus(ProcessingStatus.DISPATCHED);
                    entry.setLastRetryAt(Instant.now());
                } catch (Exception e) {
                    log.error("Failed to dispatch outbox entry {} for submission {}: {}",
                            entry.getId(), entry.getSubmissionId(), e.getMessage());
                    entry.setRetryCount(entry.getRetryCount() + 1);
                    entry.setLastRetryAt(Instant.now());
                    meterRegistry.counter("processing.outbox.dispatch_failed").increment();
                }
                outboxRepository.save(entry);
            }
        } catch (Exception e) {
            log.error("Outbox poller failed: {}", e.getMessage(), e);
        }
    }
}
