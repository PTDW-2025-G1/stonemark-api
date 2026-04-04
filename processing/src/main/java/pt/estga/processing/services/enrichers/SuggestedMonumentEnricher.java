package pt.estga.processing.services.enrichers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.intake.entities.MarkEvidenceSubmission;
import pt.estga.monument.Monument;
import pt.estga.monument.services.MonumentQueryService;
import pt.estga.processing.entities.DraftMarkEvidence;
import pt.estga.processing.services.draft.DraftMarkEvidenceCommandService;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;

/**
 * Suggests a nearby Monument based on submission location if available.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuggestedMonumentEnricher implements Enricher {

    private final DraftMarkEvidenceQueryService draftQueryService;
    private final DraftMarkEvidenceCommandService draftCommandService;
    private final MonumentQueryService monumentQueryService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrich(Long draftId) {
        draftQueryService.findById(draftId).ifPresentOrElse(draft -> {
            MarkEvidenceSubmission submission = draft.getSubmission();
            if (submission == null || submission.getLatitude() == null || submission.getLongitude() == null) {
                log.debug("Draft {} has no location — skipping monument suggestion", draftId);
                return;
            }

            try {
                // Build a simple bounding box around the submission location (approx ±0.01 degrees ~ ~1km)
                double lat = submission.getLatitude();
                double lon = submission.getLongitude();
                double delta = 0.01;
                String polygon = String.format(
                        "{\"type\":\"Polygon\",\"coordinates\":[[[%f,%f],[%f,%f],[%f,%f],[%f,%f],[%f,%f]]]}",
                        lon - delta, lat - delta,
                        lon + delta, lat - delta,
                        lon + delta, lat + delta,
                        lon - delta, lat + delta,
                        lon - delta, lat - delta
                );

                var page = monumentQueryService.findByPolygon(polygon, PageRequest.of(0, 1));
                if (page != null && page.hasContent()) {
                    Monument m = page.getContent().get(0);
                    draft.setSuggestedMonument(m);
                    draftCommandService.update(draft);
                    log.info("Suggested monument {} for draft {}", m.getId(), draftId);
                }
            } catch (Exception e) {
                log.warn("SuggestedMonumentEnricher failed for draft {}", draftId, e);
            }
        }, () -> log.warn("Draft with id {} not found for suggested monument enricher", draftId));
    }
}

