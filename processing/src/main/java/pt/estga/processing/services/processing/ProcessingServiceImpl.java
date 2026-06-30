package pt.estga.processing.services.processing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.estga.commoncore.utils.VectorUtils;
import pt.estga.file.api.FileStorageOperations;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.services.similarity.SimilarityService;
import pt.estga.vision.VisionClient;
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

    private final MarkEvidenceSubmissionRepository submissionRepository;
    private final MarkEvidenceProcessingRepository processingRepository;
    private final VisionClient visionClient;
    private final FileStorageOperations fileStorage;
    private final SimilarityService similarityService;
    private final MeterRegistry meterRegistry;
    private final ProcessingPersistenceService persistence;

    @Value("${processing.vision.max-concurrency:4}")
    private int visionMaxConcurrency;

    @Value("${processing.vision.acquire-timeout-ms:10000}")
    private long visionAcquireTimeoutMs;

    private final Semaphore visionSemaphore = new Semaphore(Math.max(1, 4));

    {
        visionSemaphore.release(visionSemaphore.availablePermits());
        // reseed on construction; @Value not yet injected, so init in first call
    }

    private volatile boolean semaphoreInitialized;

    private void ensureSemaphoreInitialized() {
        if (!semaphoreInitialized) {
            synchronized (this) {
                if (!semaphoreInitialized) {
                    this.visionSemaphore.drainPermits();
                    this.visionSemaphore.release(Math.max(1, visionMaxConcurrency));
                    semaphoreInitialized = true;
                }
            }
        }
    }

    @Override
    public void processSubmission(Long submissionId) {
        submissionRepository.findById(submissionId).ifPresentOrElse(submission -> {
            ensureSemaphoreInitialized();
            MarkEvidenceProcessing processing = null;
            long startNanos = System.nanoTime();
            try {
                processing = persistence.createOrReuseProcessingRecord(submissionId);
                if (processing == null) {
                    meterRegistry.counter("processing.submissions.skipped", "reason", "already_processed_or_in_progress").increment();
                    return;
                }

                meterRegistry.counter("processing.submissions.attempts").increment();

                if (!visionClient.isAvailable()) {
                    log.warn("Vision unavailable, deferring processing {}", submissionId);
                    persistence.setPending(processing.getId());
                    meterRegistry.counter("processing.submissions.skipped", "reason", "vision_unavailable").increment();
                    return;
                }

                UUID mediaFileId = submission.getOriginalMediaFileId();
                if (mediaFileId == null || !fileStorage.existsById(mediaFileId)) {
                    persistence.setFailed(processing.getId(),
                            "Original media file not found: " + mediaFileId, true, startNanos);
                    return;
                }

                boolean acquired = false;
                try {
                    try {
                        acquired = visionSemaphore.tryAcquire(visionAcquireTimeoutMs, TimeUnit.MILLISECONDS);
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

                    float[] embedding;
                    try (InputStream in = fileStorage.openStream(mediaFileId)
                            .orElseThrow(() -> new IllegalStateException("Media file " + mediaFileId + " not found for streaming"))) {
                        var detection = visionClient.detectMark(in,
                                fileStorage.findById(mediaFileId).map(mf -> mf.originalFilename()).orElse("unknown"));
                        embedding = detection.embedding();
                    }

                    if (embedding == null || embedding.length == 0) {
                        persistence.setFailed(processing.getId(), "Empty embedding returned", true, startNanos);
                        return;
                    }

                    float[] normalized = VectorUtils.normalize(embedding);
                    if (normalized == null) {
                        persistence.setFailed(processing.getId(), "Embedding has zero norm", true, startNanos);
                        return;
                    }

                    processing.setEmbedding(normalized);
                } finally {
                    if (acquired) {
                        visionSemaphore.release();
                    }
                }

                List<MarkSuggestion> suggestions = similarityService.findSimilar(processing, 20);
                persistence.finalizeSuccess(processing.getId(), processing.getEmbedding(), suggestions, startNanos);

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
}
