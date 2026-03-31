package pt.estga.processing.services.enrichers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.services.application.MediaService;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.vision.DetectionResult;
import pt.estga.vision.VisionClient;

import java.io.InputStream;

/**
 * Enricher responsible for extracting embeddings from submission media using
 * the Vision service and persisting the result to the submission record.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingEnricher implements Enricher {

    private final MarkEvidenceSubmissionRepository markEvidenceSubmissionRepository;
    private final VisionClient visionClient;
    private final MediaService mediaService;

    /**
     * Run detection and persist embeddings. Uses a separate transaction so
     * failures here won't affect other enrichers or the origin transaction.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrich(Long submissionId) {
        markEvidenceSubmissionRepository.findById(submissionId).ifPresentOrElse(submission -> {
            if (submission.getOriginalMediaFile() == null) {
                log.info("Submission ID {} has no original media file. Skipping embedding enrichment.", submissionId);
                return;
            }

            try (InputStream detectionInputStream = mediaService.loadFileById(submission.getOriginalMediaFile().getId()).getInputStream()) {
                DetectionResult detectionResult = visionClient.detect(detectionInputStream, submission.getOriginalMediaFile().getOriginalFilename());
                if (detectionResult != null && detectionResult.embedding() != null && detectionResult.embedding().length > 0) {
                    submission.setEmbedding(detectionResult.embedding());
                    markEvidenceSubmissionRepository.save(submission);
                    log.info("Embedding updated for submission ID: {}", submissionId);
                } else {
                    log.info("No embedding detected for submission ID: {}", submissionId);
                }
            } catch (Exception e) {
                log.warn("Detection service failed for submission ID {}. Proceeding without detection.", submissionId, e);
            }
        }, () -> log.warn("Submission with ID {} not found during embedding enrichment", submissionId));
    }
}
