package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.intake.services.MarkEvidenceSubmissionQueryService;
import pt.estga.processing.entities.MarkEvidenceProcessing;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.processing.repositories.MarkEvidenceProcessingRepository;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.vision.VisionClient;
import pt.estga.file.services.MediaContentService;

import java.io.InputStream;
import java.util.Optional;
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

    @Transactional
    @Override
    public void processSubmission(Long submissionId) {
        submissionQueryService.findById(submissionId).ifPresentOrElse(submission -> {
            try {
                // Idempotency: load existing processing if present
                var existingOpt = processingRepository.findBySubmissionId(submissionId);
                MarkEvidenceProcessing processing;

                if (existingOpt.isPresent()) {
                    processing = existingOpt.get();
                    if (processing.getStatus() == ProcessingStatus.COMPLETED || processing.getStatus() == ProcessingStatus.PROCESSING) {
                        log.info("Submission {} already processed or in progress (status={}), skipping", submissionId, processing.getStatus());
                        return;
                    }
                    // reuse the existing failed or pending processing
                    processing.setStatus(ProcessingStatus.PROCESSING);
                    processing.setFailedAt(null);
                    processing.setErrorMessage(null);
                    processingRepository.save(processing);
                } else {
                    processing = MarkEvidenceProcessing.builder()
                            .submission(submission)
                            .status(ProcessingStatus.PROCESSING)
                            .build();

                    processing = processingRepository.save(processing);
                }

                // Check vision availability
                if (!visionClient.isAvailable()) {
                    processing.setStatus(ProcessingStatus.FAILED);
                    processing.setFailedAt(Instant.now());
                    processing.setErrorMessage("Vision service unavailable");
                    processingRepository.save(processing);
                    return;
                }

                // Load content stream for detection
                var mediaFile = submission.getOriginalMediaFile();
                if (mediaFile == null) {
                    processing.setStatus(ProcessingStatus.FAILED);
                    processing.setFailedAt(Instant.now());
                    processing.setErrorMessage("No original media file available");
                    processingRepository.save(processing);
                    return;
                }

                try (InputStream in = mediaContentService.loadContent(mediaFile.getStoragePath()).getInputStream()) {
                    var detection = visionClient.detectMark(in, mediaFile.getOriginalFilename());
                    float[] embedding = detection.embedding();

                    processing.setEmbedding(embedding);
                    processingRepository.save(processing);

                    // Run similarity and persist suggestions
                    List<MarkSuggestion> suggestions = similarityService.findSimilar(processing, 20);
                    if (!suggestions.isEmpty()) {
                        suggestionRepository.saveAll(suggestions);
                    }

                    processing.setStatus(ProcessingStatus.COMPLETED);
                    processing.setProcessedAt(Instant.now());
                    processingRepository.save(processing);
                }

            } catch (Exception e) {
                log.error("Error processing submission {}: {}", submissionId, e.getMessage(), e);
                // Try to record failure state
                var p = processingRepository.findBySubmissionId(submissionId).orElse(null);
                if (p != null) {
                    p.setStatus(ProcessingStatus.FAILED);
                    p.setFailedAt(Instant.now());
                    p.setErrorMessage(e.getMessage());
                    processingRepository.save(p);
                }
            }
        }, () -> log.warn("Submission with id {} not found for processing", submissionId));
    }
}
