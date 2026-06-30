package pt.estga.processing.services.processing;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.LockTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingPersistenceService {

    @Lazy
    @Autowired
    private ProcessingPersistenceService self;

    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkSuggestionRepository suggestionRepository;
    private final MeterRegistry meterRegistry;

    /**
     * Create a processing record or reuse an existing one.
     * Returns null when no work should be performed (idempotency).
     */
    @Transactional
    public MarkEvidenceProcessing createOrReuseProcessingRecord(Long submissionId) {
        var overview = processingRepository.findOverviewBySubmissionId(submissionId);
        if (overview.isPresent()) {
            ProcessingStatus status = overview.get().getStatus();
            if (status == ProcessingStatus.COMPLETED || status == ProcessingStatus.PROCESSING) {
                log.info("Submission {} already processed or in progress (status={}), skipping", submissionId, status);
                return null;
            }
        }

        try {
            var existingOpt = processingRepository.findBySubmissionIdForUpdate(submissionId);
            if (existingOpt.isPresent()) {
                MarkEvidenceProcessing p = existingOpt.get();
                if (p.getStatus() == ProcessingStatus.COMPLETED || p.getStatus() == ProcessingStatus.PROCESSING) {
                    log.info("Submission {} already processed or in progress (status={}), skipping", submissionId, p.getStatus());
                    return null;
                }
                p.setStatus(ProcessingStatus.PROCESSING);
                p.setFailedAt(null);
                p.setErrorMessage(null);
                p.setUpdatedAt(Instant.now());
                p.setProcessingStartedAt(Instant.now());
                return processingRepository.save(p);
            }

            MarkEvidenceProcessing p = MarkEvidenceProcessing.builder()
                    .submissionId(submissionId)
                    .status(ProcessingStatus.PROCESSING)
                    .updatedAt(Instant.now())
                    .processingStartedAt(Instant.now())
                    .build();
            try {
                return processingRepository.save(p);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                log.warn("Race detected creating processing for submission {} — retrying", submissionId);
                return self.handleRace(submissionId);
            }
        } catch (LockTimeoutException | LockAcquisitionException lockEx) {
            log.warn("Could not acquire lock for submission {}: {}", submissionId, lockEx.getMessage());
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected MarkEvidenceProcessing handleRace(Long submissionId) {
        var found = processingRepository.findBySubmissionId(submissionId);
        if (found.isPresent()) {
            var existing = found.get();
            if (existing.getStatus() == ProcessingStatus.COMPLETED
                    || existing.getStatus() == ProcessingStatus.PROCESSING) {
                return null;
            }
            existing.setStatus(ProcessingStatus.PROCESSING);
            existing.setFailedAt(null);
            existing.setErrorMessage(null);
            existing.setUpdatedAt(Instant.now());
            existing.setProcessingStartedAt(Instant.now());
            return processingRepository.save(existing);
        }
        throw new IllegalStateException("Race handling failed for submission " + submissionId);
    }

    @Transactional
    public void setPending(UUID processingId) {
        processingRepository.findById(processingId).ifPresent(p -> {
            p.setStatus(ProcessingStatus.PENDING);
            p.setLastRetryAt(Instant.now());
            processingRepository.save(p);
        });
    }

    @Transactional
    public void setFailed(UUID processingId, String message, boolean permanent, long startNanos) {
        processingRepository.findById(processingId).ifPresent(p -> {
            p.setStatus(ProcessingStatus.FAILED);
            p.setFailedAt(Instant.now());
            p.setLastRetryAt(Instant.now());
            p.setRetryCount(p.getRetryCount() + 1);
            p.setPermanent(permanent);
            p.setErrorMessage(message);
            processingRepository.save(p);

            meterRegistry.counter("processing.submissions.failed", "permanent", String.valueOf(permanent)).increment();
            long durationNanos = System.nanoTime() - startNanos;
            meterRegistry.timer("processing.submissions.duration", "result", "failed").record(Duration.ofNanos(durationNanos));
            long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
            log.info("Processing {} failed for submission {} after {} ms: {}", processingId, p.getSubmissionId(), durationMs, message);
        });
    }

    @Transactional
    public boolean finalizeSuccess(UUID processingId, float[] embedding, List<MarkSuggestion> suggestions, long startNanos) {
        var opt = processingRepository.findById(processingId);
        if (opt.isEmpty()) return false;

        var p = opt.get();
        if (p.getStatus() != ProcessingStatus.PROCESSING) {
            log.warn("Processing {} not in PROCESSING state (actual={}), discarding stale completion",
                    processingId, p.getStatus());
            return false;
        }

        p.setEmbedding(embedding);
        p.setProcessedAt(Instant.now());
        p.setRetryCount(0);
        p.setLastRetryAt(null);
        p.setPermanent(false);
        p.setStatus(ProcessingStatus.COMPLETED);

        suggestionRepository.deleteByProcessingId(p.getId());
        if (suggestions != null && !suggestions.isEmpty()) {
            suggestions.forEach(s -> s.setProcessing(p));
            suggestions.forEach(s -> s.setId(null));
            suggestionRepository.saveAll(suggestions);
        }
        processingRepository.save(p);

        meterRegistry.counter("processing.submissions.success").increment();
        long durationNanos = System.nanoTime() - startNanos;
        meterRegistry.timer("processing.submissions.duration", "result", "success").record(Duration.ofNanos(durationNanos));
        long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
        int suggestionCount = suggestions == null ? 0 : suggestions.size();
        log.info("Processing {} completed for submission {} after {} ms — suggestions={}",
                processingId, p.getSubmissionId(), durationMs, suggestionCount);
        return true;
    }
}
