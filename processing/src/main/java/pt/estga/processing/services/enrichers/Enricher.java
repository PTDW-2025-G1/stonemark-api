package pt.estga.processing.services.enrichers;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contract for an enrichment unit that can enrich a processing draft by id.
 * <p>
 * Implementations are expected to execute enrichment in a new transaction
 * so failures in one enricher do not affect others. Annotating the contract
 * clarifies the transactional requirement for implementors and callers.
 */
public interface Enricher {
    /**
     * Enrich the provided draft identified by id. This method is required to
     * run with a REQUIRES_NEW transaction so that each enricher is isolated.
     *
     * @param draftId id of the processing draft
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void enrich(Long draftId);
}
