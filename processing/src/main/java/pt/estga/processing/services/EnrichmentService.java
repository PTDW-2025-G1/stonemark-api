package pt.estga.processing.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.services.enrichers.Enricher;

import java.util.List;

/**
 * Orchestrates multiple enrichment strategies for a single submission.
 * <p>
 * This service delegates to one or more {@link Enricher} implementations. Each
 * enricher runs in its own REQUIRES_NEW transaction so failures in one
 * enrichment do not affect others.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentService {

    private final List<Enricher> enrichers;

    /**
     * Enrich the submission by delegating to configured enrichers.
     * The method is executed with REQUIRES_NEW to ensure it runs after the
     * origin transaction commits. Individual enrichers also run in their own
     * REQUIRES_NEW transactions.
     *
     * @param submissionId id of the submission to enrich
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrichSubmission(Long submissionId) {
        for (Enricher enricher : enrichers) {
            try {
                enricher.enrich(submissionId);
            } catch (Exception e) {
                log.warn("Enricher {} failed for submission {} - continuing with next enricher", enricher.getClass().getSimpleName(), submissionId, e);
            }
        }
    }
}
