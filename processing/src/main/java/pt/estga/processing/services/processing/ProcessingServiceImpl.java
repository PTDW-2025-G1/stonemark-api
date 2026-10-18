package pt.estga.processing.services.processing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.estga.shared.core.utils.VectorUtils;
import pt.estga.shared.core.models.EmbeddingVector;
import pt.estga.file.api.MediaOperations;
import pt.estga.intake.api.IntakeSubmissionApi;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.services.similarity.SimilarityStrategy;
import pt.estga.processing.vision.DetectionResult;
import pt.estga.processing.vision.VisionClient;
import io.micrometer.core.instrument.MeterRegistry;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingServiceImpl implements ProcessingService {

    private final IntakeSubmissionApi intakeApi;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final VisionClient visionClient;
    private final MediaOperations fileStorage;
    private final SimilarityStrategy similarityStrategy;
    private final MeterRegistry meterRegistry;
    private final ProcessingPersistenceService persistence;

    @Value("${processing.vision.max-concurrency:4}")
    private int visionMaxConcurrency;

    @Value("${processing.vision.acquire-timeout-ms:10000}")
    private long visionAcquireTimeoutMs;

    private volatile Semaphore visionSemaphore;

    private Semaphore getVisionSemaphore() {
        if (visionSemaphore == null) {
            synchronized (this) {
                if (visionSemaphore == null) {
                    visionSemaphore = new Semaphore(Math.max(1, visionMaxConcurrency));
                }
            }
        }
        return visionSemaphore;
    }

    @Override
    public void processSubmission(UUID submissionId) {
        intakeApi.findById(submissionId).ifPresentOrElse(submission -> {
            MarkEvidenceProcessing processing = null;
            long startNanos = System.nanoTime();
            try {
                processing = persistence.createOrReuseProcessingRecord(submissionId);
                if (processing == null) {
                    meterRegistry.counter("processing.submissions.skipped", "reason", notClaimedReason(submissionId)).increment();
                    return;
                }

                meterRegistry.counter("processing.submissions.attempts").increment();

                if (!visionClient.isAvailable()) {
                    log.warn("Vision unavailable, deferring processing {}", submissionId);
                    persistence.setPending(processing.getId());
                    meterRegistry.counter("processing.submissions.skipped", "reason", "vision_unavailable").increment();
                    return;
                }

                UUID mediaFileId = submission.originalMediaFileId();
                if (mediaFileId == null || !fileStorage.existsById(mediaFileId)) {
                    persistence.setFailed(processing.getId(),
                            "Original media file not found: " + mediaFileId, true, startNanos);
                    return;
                }

                boolean acquired = false;
                try {
                    try {
                        acquired = getVisionSemaphore().tryAcquire(visionAcquireTimeoutMs, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted waiting for vision permit, deferring processing {}", submissionId);
                        persistence.setPending(processing.getId());
                        meterRegistry.counter("processing.submissions.throttled").increment();
                        return;
                    }

                    if (!acquired) {
                        log.warn("Could not obtain vision permit in {}ms, deferring processing {}", visionAcquireTimeoutMs, submissionId);
                        persistence.setPending(processing.getId());
                        meterRegistry.counter("processing.submissions.throttled").increment();
                        return;
                    }

                    EmbeddingVector embeddingVector;
                    try (InputStream in = fileStorage.openStream(mediaFileId)
                            .orElseThrow(() -> new IllegalStateException("Media file " + mediaFileId + " not found for streaming"))) {
                        DetectionResult detection = visionClient.detectMark(in,
                                fileStorage.findById(mediaFileId).map(mf -> mf.originalFilename()).orElse("unknown"));

                        float[] embedding = detection.embedding();
                        if (embedding == null || embedding.length == 0) {
                            persistence.setFailed(processing.getId(), "Empty embedding returned", true, startNanos);
                            return;
                        }

                        float[] normalized = VectorUtils.normalize(embedding);
                        if (normalized == null) {
                            persistence.setFailed(processing.getId(), "Embedding has zero norm", true, startNanos);
                            return;
                        }

                        embeddingVector = new EmbeddingVector(
                                normalized,
                                detection.embeddingModel(),
                                detection.embeddingVersion(),
                                detection.dim() != null ? detection.dim() : normalized.length);
                        processing.setEmbedding(normalized);
                    }

                    List<MarkSuggestion> suggestions = similarityStrategy.findSimilar(processing, 20);
                    persistence.finalizeSuccess(processing.getId(), embeddingVector, suggestions, startNanos);
                } finally {
                    if (acquired) {
                        getVisionSemaphore().release();
                    }
                }

            } catch (Exception e) {
                log.error("Error processing submission {}: {}", submissionId, e.getMessage(), e);
                try {
                    if (processing != null) {
                        persistence.setFailed(processing.getId(), e.getMessage(), false, startNanos);
                    } else {
                        processingRepository.findBySubmissionId(submissionId)
                                .ifPresent(p -> persistence.setFailed(p.getId(), e.getMessage(), false, startNanos));
                    }
                } catch (Exception ex) {
                    log.warn("Failed to persist failure state for submission {}: {}", submissionId, ex.getMessage());
                }
            }
        }, () -> log.warn("Submission with id {} not found for processing", submissionId));
    }

    /**
     * A dispatch that could not claim a record is either already processed/in
     * progress or was blocked by a lock; the record's current state tells which.
     */
    private String notClaimedReason(UUID submissionId) {
        return processingRepository.findBySubmissionId(submissionId)
                .map(record -> "status_" + record.getStatus().name().toLowerCase())
                .orElse("not_claimed");
    }
}
