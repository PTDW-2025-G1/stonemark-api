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

import java.io.InputStream;
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

    @Override
    public void processSubmission(Long submissionId) {
        submissionQueryService.findById(submissionId).ifPresentOrElse(submission -> {
            MarkEvidenceProcessing processing = null;
            try {
                // Phase A: short DB work - create or reuse processing record
                processing = createOrReuseProcessingRecord(submissionId, submission);

                // Phase B: external work (outside transaction)
                if (!visionClient.isAvailable()) {
                    log.warn("Vision unavailable, skipping processing {}", submissionId);
                    // revert to PENDING so it can be retried later
                    setProcessingPending(processing.getId());
                    return;
                }

                var mediaFile = submission.getOriginalMediaFile();
                if (mediaFile == null) {
                    setProcessingFailed(processing.getId(), "No original media file available");
                    return;
                }

                float[] embedding;
                try (InputStream in = mediaContentService.loadContent(mediaFile.getStoragePath()).getInputStream()) {
                    var detection = visionClient.detectMark(in, mediaFile.getOriginalFilename());
                    embedding = detection.embedding();
                }

                if (embedding == null || embedding.length == 0) {
                    setProcessingFailed(processing.getId(), "Empty embedding returned");
                    return;
                }

                // Attach embedding to the in-memory processing object for similarity search
                processing.setEmbedding(embedding);

                // Run similarity outside transaction
                List<MarkSuggestion> suggestions = similarityService.findSimilar(processing, 20);

                // Phase C: short DB work - persist embedding, suggestions and mark completed
                finalizeProcessingSuccess(processing.getId(), embedding, suggestions);

            } catch (Exception e) {
                log.error("Error processing submission {}: {}", submissionId, e.getMessage(), e);
                try {
                    if (processing != null) {
                        setProcessingFailed(processing.getId(), e.getMessage());
                    } else {
                        // best-effort: find by submission and mark failed
                        var p = processingRepository.findBySubmissionId(submissionId).orElse(null);
                        if (p != null) {
                            setProcessingFailed(p.getId(), e.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Failed to persist failure state for submission {}: {}", submissionId, ex.getMessage());
                }
            }
        }, () -> log.warn("Submission with id {} not found for processing", submissionId));
    }

    // --- helper DB operations (short transactions executed by repository methods) ---

    protected MarkEvidenceProcessing createOrReuseProcessingRecord(Long submissionId, MarkEvidenceSubmission submission) {
        var existingOpt = processingRepository.findBySubmissionId(submissionId);
        if (existingOpt.isPresent()) {
            MarkEvidenceProcessing p = existingOpt.get();
            if (p.getStatus() == ProcessingStatus.COMPLETED || p.getStatus() == ProcessingStatus.PROCESSING) {
                log.info("Submission {} already processed or in progress (status={}), skipping", submissionId, p.getStatus());
                return p;
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

    protected void setProcessingFailed(UUID processingId, String message) {
        processingRepository.findById(processingId).ifPresent(p -> {
            p.setStatus(ProcessingStatus.FAILED);
            p.setFailedAt(Instant.now());
            p.setErrorMessage(message);
            processingRepository.save(p);
        });
    }

    protected void finalizeProcessingSuccess(UUID processingId, float[] embedding, List<MarkSuggestion> suggestions) {
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
        });
    }
}
