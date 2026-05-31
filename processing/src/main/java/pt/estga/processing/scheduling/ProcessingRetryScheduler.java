package pt.estga.processing.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.services.processing.AsyncProcessingService;
import pt.estga.vision.VisionClient;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingRetryScheduler {

    private final MarkEvidenceProcessingRepository processingRepository;
    private final AsyncProcessingService asyncProcessingService;
    private final VisionClient visionClient;
    private final MeterRegistry meterRegistry;

    @Value("${processing.retry.base-delay-ms:60000}")
    private long baseDelayMs;

    @Value("${processing.retry.max-delay-ms:1800000}")
    private long maxDelayMs;

    @Value("${processing.retry.batch-size:5}")
    private int batchSize;

    @Value("${processing.retry.batch-pause-ms:2000}")
    private long batchPauseMs;

    @Scheduled(fixedDelayString = "${processing.retry.interval:60000}")
    public void retryPending() {
        try {
            if (!visionClient.isAvailable()) {
                log.debug("Vision service unavailable — skipping retry cycle");
                meterRegistry.counter("processing.retry.skipped", "reason", "vision_unavailable").increment();
                return;
            }

            var retryable = processingRepository.findRetryableByStatusIn(
                    List.of(ProcessingStatus.PENDING, ProcessingStatus.FAILED));
            if (retryable == null || retryable.isEmpty()) {
                return;
            }

            var dueNow = retryable.stream()
                    .filter(this::isBackoffElapsed)
                    .toList();

            int skipped = retryable.size() - dueNow.size();
            if (skipped > 0) {
                log.debug("Skipping {} entries still in backoff ({} due now)", skipped, dueNow.size());
                meterRegistry.counter("processing.retry.backoff_skipped").increment(skipped);
            }

            if (dueNow.isEmpty()) {
                return;
            }

            log.info("Retrying {} processing entries in batches of {}", dueNow.size(), batchSize);
            meterRegistry.counter("processing.retry.invocations").increment();
            dispatchInBatches(dueNow);

        } catch (Exception e) {
            log.error("Error while retrying pending processing entries: {}", e.getMessage(), e);
        }
    }

    private boolean isBackoffElapsed(MarkEvidenceProcessing p) {
        if (p.getLastRetryAt() == null) {
            return true;
        }
        long delayMs = computeBackoff(p.getRetryCount());
        Instant nextAttempt = p.getLastRetryAt().plus(Duration.ofMillis(delayMs));
        return Instant.now().isAfter(nextAttempt);
    }

    private long computeBackoff(int retryCount) {
        long delay = baseDelayMs * (1L << Math.min(retryCount, 10));
        return Math.min(delay, maxDelayMs);
    }

    private void dispatchInBatches(List<MarkEvidenceProcessing> entries) {
        for (int i = 0; i < entries.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entries.size());
            var batch = entries.subList(i, end);

            for (var p : batch) {
                Long submissionId = p.getSubmissionId();
                if (submissionId == null) {
                    log.warn("Skipping processing entry {} with no submission linked", p.getId());
                    continue;
                }
                meterRegistry.counter("processing.retry.scheduled").increment();
                asyncProcessingService.processAsync(submissionId);
            }

            if (end < entries.size()) {
                try {
                    Thread.sleep(batchPauseMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
