package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.services.application.MediaService;
import pt.estga.intake.repositories.MarkEvidenceSubmissionRepository;
import pt.estga.vision.DetectionResult;
import pt.estga.vision.VisionClient;

import java.io.InputStream;

/**
 * Service responsible for enriching mark evidence submissions by running
 * vision detection and persisting any produced embeddings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentService {

    private final MarkEvidenceSubmissionRepository markEvidenceSubmissionRepository;
    private final VisionClient visionClient;
    private final MediaService mediaService;

    /**
     * Enriches the submission identified by the provided id. This method runs
     * in a new transaction so it can safely execute after the origin transaction
     * has committed.
     *
     * @param submissionId id of the submission to enrich
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrichSubmission(Long submissionId) {
        markEvidenceSubmissionRepository.findById(submissionId).ifPresentOrElse(submission -> {
            if (submission.getOriginalMediaFile() == null) {
                log.info("Submission ID {} has no original media file. Skipping enrichment.", submissionId);
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
        }, () -> log.warn("Submitted submission with ID {} not found during enrichment", submissionId));
    }
}

