package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import pt.estga.vision.VisionClient;
import pt.estga.processing.services.draft.DraftMarkEvidenceQueryService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrichmentOrchestrator {

    private final VisionClient visionClient;
    private final DraftMarkEvidenceQueryService draftQueryService;
    private final EnrichmentService enrichmentService;

    @Value("${processing.polling-rate-ms:10000}")
    private long pollingRateMs; // kept for external config visibility

    private static final int BATCH_SIZE = 10;

    @Scheduled(fixedDelayString = "${processing.polling-rate-ms:10000}")
    public void pollAndProcess() {
        try {
            if (!visionClient.isAvailable()) {
                log.debug("Vision server unavailable - skipping processing cycle");
                return;
            }

            List<Long> ids = draftQueryService.findSubmissionsReadyForProcessing(PageRequest.of(0, BATCH_SIZE));
            if (ids == null || ids.isEmpty()) {
                log.debug("No submissions ready for processing in this cycle");
                return;
            }

            log.info("Processing {} drafts from queue", ids.size());
            ids.forEach(id -> {
                try {
                    enrichmentService.enrichSubmission(id);
                } catch (Exception e) {
                    log.warn("Failed to trigger enrichment for submission {}", id, e);
                }
            });

        } catch (Exception e) {
            log.warn("Error during enrichment orchestrator run", e);
        }
    }
}
