package pt.estga.processing.services.processing;

import jakarta.persistence.LockTimeoutException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.stereotype.Service;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import java.util.UUID;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.services.similarity.SimilarityService;
import pt.estga.processing.services.suggestions.MarkSuggestionCommandService;
import pt.estga.vision.VisionClient;
import pt.estga.file.services.MediaContentService;
import io.micrometer.core.instrument.MeterRegistry;

import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingServiceImpl implements ProcessingService {

    private final MarkEvidenceSubmissionQueryService submissionQueryService;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkSuggestionCommandService suggestionCommandService;
    private final VisionClient visionClient;
    private final MediaContentService mediaContentService;
    private final SimilarityService similarityService;
    private final MeterRegistry meterRegistry;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @Value("${processing.vision.max-concurrency:4}")
    private int visionMaxConcurrency;

    @Value("${processing.vision.acquire-timeout-ms:10000}")
    private long visionAcquireTimeoutMs;

    private Semaphore visionSemaphore;

    @PostConstruct
    protected void init() {
        this.visionSemaphore = new Semaphore(Math.max(1, visionMaxConcurrency));
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void processSubmission(Long submissionId) {
        submissionQueryService.findById(submissionId).ifPresentOrElse(submission -> {
            MarkEvidenceProcessing processing = null;
            long startNanos = System.nanoTime();
            try {
                // Phase A: short DB work - create or reuse processing record
                processing = createOrReuseProcessingRecord(submissionId, submission);

                // createOrReuseProcessingRecord returns null when work should be skipped (idempotency)
                if (processing == null) {
                    meterRegistry.counter("processing.submissions.skipped", "reason", "already_processed_or_in_progress").increment();
                    return;
                }

                // record an attempt
                meterRegistry.counter("processing.submissions.attempts").increment();

                // Phase B: external work (outside transaction)
                if (!visionClient.isAvailable()) {
                    log.warn("Vision unavailable, skipping processing {}", submissionId);
                    // revert to PENDING so it can be retried later
                    setProcessingPending(processing.getId());
                    meterRegistry.counter("processing.submissions.skipped", "reason", "vision_unavailable").increment();
                    return;
                }

                var mediaFile = submission.getOriginalMediaFile();
                if (mediaFile == null) {
                    setProcessingFailed(processing.getId(), "No original media file available", true, startNanos);
                    return;
                }

                // Apply backpressure for Vision calls: limit concurrent detection requests using a semaphore.
                boolean acquired = false;
                try {
                    try {
                        acquired = visionSemaphore.tryAcquire(visionAcquireTimeoutMs, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while waiting for vision permit, deferring processing {}", submissionId);
                        setProcessingPending(processing.getId());
                        meterRegistry.counter("processing.submissions.throttled").increment();
                        return;
                    }

                    if (!acquired) {
                        log.warn("Could not obtain vision permit in {}ms, deferring processing {}", visionAcquireTimeoutMs, submissionId);
                        setProcessingPending(processing.getId());
                        meterRegistry.counter("processing.submissions.throttled").increment();
                        return;
                    }

                    float[] embedding;
                    try (InputStream in = mediaContentService.loadContent(mediaFile.getStoragePath()).getInputStream()) {
                        var detection = visionClient.detectMark(in, mediaFile.getOriginalFilename());
                        embedding = detection.embedding();
                    }

                    if (embedding == null || embedding.length == 0) {
                        setProcessingFailed(processing.getId(), "Empty embedding returned", true, startNanos);
                        return;
                    }

                    // Attach embedding to the in-memory processing object for similarity search
                    processing.setEmbedding(embedding);
                } finally {
                    if (acquired) {
                        visionSemaphore.release();
                    }
                }

                // Run similarity outside transaction
                List<MarkSuggestion> suggestions = similarityService.findSimilar(processing, 20);

                // Phase C: short DB work - persist embedding, suggestions and mark completed
                finalizeProcessingSuccess(processing.getId(), processing.getEmbedding(), suggestions, startNanos);

            } catch (Exception e) {
                log.error("Error processing submission {}: {}", submissionId, e.getMessage(), e);
                try {
                    if (processing != null) {
                        setProcessingFailed(processing.getId(), e.getMessage(), false, startNanos);
                    } else {
                        // best-effort: find by submission and mark failed (retryable)
                        processingRepository.findBySubmissionId(submissionId)
                                .ifPresent(p -> setProcessingFailed(p.getId(), e.getMessage(), false, startNanos));
                    }
                } catch (Exception ex) {
                    log.warn("Failed to persist failure state for submission {}: {}", submissionId, ex.getMessage());
                }
            }
        }, () -> log.warn("Submission with id {} not found for processing", submissionId));
    }

    // --- helper DB operations (short transactions executed by repository methods) ---

    /**
     * Create a processing record or reuse an existing one.
     * Returns null when no work should be performed (idempotency - already completed or in progress).
     */
    protected MarkEvidenceProcessing createOrReuseProcessingRecord(Long submissionId, MarkEvidenceSubmission submission) {
        return transactionTemplate.execute(status -> {
            // Try to fetch existing record with a pessimistic lock to avoid races between concurrent processors.
            try {
                var existingOpt = processingRepository.findBySubmissionIdForUpdate(submissionId);
                if (existingOpt.isPresent()) {
                    MarkEvidenceProcessing p = existingOpt.get();
                    if (p.getStatus() == ProcessingStatus.COMPLETED || p.getStatus() == ProcessingStatus.PROCESSING) {
                        log.info("Submission {} already processed or in progress (status={}), skipping", submissionId, p.getStatus());
                        return null; // signal caller to skip work
                    }
                    p.setStatus(ProcessingStatus.PROCESSING);
                    p.setFailedAt(null);
                    p.setErrorMessage(null);
                    return processingRepository.save(p);
                }

                // No existing record; attempt to create one. Another concurrent inserter may produce a unique constraint
                // violation; handle it gracefully by reading that record and deciding what to do next.
                MarkEvidenceProcessing p = MarkEvidenceProcessing.builder()
                        .submission(submission)
                        .status(ProcessingStatus.PROCESSING)
                        .build();
                try {
                    return processingRepository.save(p);
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                    log.warn("Race detected creating processing for submission {} — reading existing record", submissionId);
                    // Read the existing record (no lock needed here; it was just created by the raced transaction).
                    return processingRepository.findBySubmissionId(submissionId).map(existing -> {
                        if (existing.getStatus() == ProcessingStatus.COMPLETED || existing.getStatus() == ProcessingStatus.PROCESSING) {
                            return null;
                        }
                        existing.setStatus(ProcessingStatus.PROCESSING);
                        existing.setFailedAt(null);
                        existing.setErrorMessage(null);
                        return processingRepository.save(existing);
                    }).orElseThrow(() -> ex);
                }
            } catch (LockTimeoutException | LockAcquisitionException lockEx) {
                // Lock acquisition failed — treat as a retryable condition by skipping work now; the scheduler will retry later.
                log.warn("Could not acquire lock for submission {}: {}", submissionId, lockEx.getMessage());
                return null;
            }
        });
    }

    protected void setProcessingPending(UUID processingId) {
        processingRepository.findById(processingId).ifPresent(p -> {
            p.setStatus(ProcessingStatus.PENDING);
            processingRepository.save(p);
        });
    }

    /**
     * Mark processing as failed. 'permanent' indicates whether the failure is non-retryable.
     * Records metrics (failure counter and processing duration).
     */
    protected void setProcessingFailed(UUID processingId, String message, boolean permanent, long startNanos) {
        processingRepository.findById(processingId).ifPresent(p -> {
            p.setStatus(ProcessingStatus.FAILED);
            p.setFailedAt(Instant.now());
            p.setErrorMessage(message);
            processingRepository.save(p);

            // metrics
            meterRegistry.counter("processing.submissions.failed", "permanent", String.valueOf(permanent)).increment();
            long durationNanos = System.nanoTime() - startNanos;
            meterRegistry.timer("processing.submissions.duration", "result", "failed").record(Duration.ofNanos(durationNanos));
            // Observability: log duration and that it failed
            long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
            Long submissionId = p.getSubmission() != null ? p.getSubmission().getId() : null;
            log.info("Processing {} failed for submission {} after {} ms: {}", processingId, submissionId, durationMs, message);
        });
    }

    /**
     * Finalize successful processing: persist embedding and suggestions, mark completed and record metrics.
     */
    protected void finalizeProcessingSuccess(UUID processingId, float[] embedding, List<MarkSuggestion> suggestions, long startNanos) {
        processingRepository.findById(processingId).ifPresent(p -> {
            p.setEmbedding(embedding);
            p.setStatus(ProcessingStatus.COMPLETED);
            p.setProcessedAt(Instant.now());
            // remove previous suggestions to avoid duplicates on reprocessing
            suggestionCommandService.deleteByProcessingId(p.getId());
            if (suggestions != null && !suggestions.isEmpty()) {
                // Ensure each suggestion references the managed processing entity
                suggestions.forEach(s -> s.setProcessing(p));
                // Persist suggestions in batch
                suggestionCommandService.createAll(suggestions);
            }
            processingRepository.save(p);

            // metrics
            meterRegistry.counter("processing.submissions.success").increment();
            long durationNanos = System.nanoTime() - startNanos;
            meterRegistry.timer("processing.submissions.duration", "result", "success").record(Duration.ofNanos(durationNanos));
            // Observability: log duration and suggestion count
            long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
            int suggestionCount = suggestions == null ? 0 : suggestions.size();
            Long submissionId = p.getSubmission() != null ? p.getSubmission().getId() : null;
            log.info("Processing {} completed for submission {} after {} ms — suggestions={}", processingId, submissionId, durationMs, suggestionCount);
        });
    }
}
