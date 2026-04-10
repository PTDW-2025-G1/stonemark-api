package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import java.util.UUID;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.vision.VisionClient;
import pt.estga.file.services.MediaContentService;
import io.micrometer.core.instrument.MeterRegistry;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingServiceImpl implements ProcessingService {

    private final MarkEvidenceSubmissionQueryService submissionQueryService;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final MarkSuggestionRepository suggestionRepository;
    private final VisionClient visionClient;
    private final MediaContentService mediaContentService;
    private final SimilarityService similarityService;
    private final MeterRegistry meterRegistry;

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

                // Run similarity outside transaction
                List<MarkSuggestion> suggestions = similarityService.findSimilar(processing, 20);

                // Phase C: short DB work - persist embedding, suggestions and mark completed
                finalizeProcessingSuccess(processing.getId(), embedding, suggestions, startNanos);

            } catch (Exception e) {
                log.error("Error processing submission {}: {}", submissionId, e.getMessage(), e);
                try {
                    if (processing != null) {
                        setProcessingFailed(processing.getId(), e.getMessage(), false, startNanos);
                    } else {
                        // best-effort: find by submission and mark failed (retryable)
                        var p = processingRepository.findBySubmissionId(submissionId).orElse(null);
                        if (p != null) {
                            setProcessingFailed(p.getId(), e.getMessage(), false, startNanos);
                        }
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
        var existingOpt = processingRepository.findBySubmissionId(submissionId);
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
        } else {
            MarkEvidenceProcessing p = MarkEvidenceProcessing.builder()
                    .submission(submission)
                    .status(ProcessingStatus.PROCESSING)
                    .build();
            return processingRepository.save(p);
        }
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
            suggestionRepository.deleteByProcessingId(p.getId());
            if (suggestions != null && !suggestions.isEmpty()) {
                suggestionRepository.saveAll(suggestions);
            }
            processingRepository.save(p);

            // metrics
            meterRegistry.counter("processing.submissions.success").increment();
            long durationNanos = System.nanoTime() - startNanos;
            meterRegistry.timer("processing.submissions.duration", "result", "success").record(Duration.ofNanos(durationNanos));
        });
    }
}
