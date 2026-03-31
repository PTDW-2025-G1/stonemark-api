package pt.estga.processing.services.enrichers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Placeholder for monument-specific enrichment logic.
 */
@Component
@Slf4j
public class MonumentEnricher implements Enricher {

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrich(Long submissionId) {
        log.debug("MonumentEnricher invoked for submission {} - no-op placeholder.", submissionId);
    }
}
