package pt.estga.processing.services.enrichers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.file.services.application.MediaService;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;
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

    private final DraftMarkEvidenceCommandService draftCommandService;
    private final DraftMarkEvidenceQueryService draftQueryService;
    private final VisionClient visionClient;
    private final MediaService mediaService;

    /**
     * Run detection and persist embeddings. Uses a separate transaction so
     * failures here won't affect other enrichers or the origin transaction.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrich(Long draftId) {
        draftQueryService.findById(draftId).ifPresentOrElse(draft -> {
            if (draft.getSubmission() == null || draft.getSubmission().getOriginalMediaFile() == null) {
                log.info("Draft {} has no linked submission media. Skipping embedding enrichment.", draftId);
                return;
            }

            try (InputStream detectionInputStream = mediaService.loadFileById(draft.getSubmission().getOriginalMediaFile().getId()).getInputStream()) {
                DetectionResult detectionResult = visionClient.detect(detectionInputStream, draft.getSubmission().getOriginalMediaFile().getOriginalFilename());
                if (detectionResult != null && detectionResult.embedding() != null && detectionResult.embedding().length > 0) {
                    draft.setEmbedding(detectionResult.embedding());
                    draftCommandService.update(draft);

                    log.info("Embedding updated for draft ID: {}", draftId);
                } else {
                    log.info("No embedding detected for draft ID: {}", draftId);
                }
            } catch (Exception e) {
                log.warn("Detection service failed for draft ID {}. Proceeding without detection.", draftId, e);
            }
        }, () -> log.warn("Draft with ID {} not found during embedding enrichment", draftId));
    }
}
