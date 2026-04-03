package pt.estga.processing.services.enrichers;

/**
 * Contract for an enrichment unit that can enrich a processing draft by id.
 */
public interface Enricher {
    /**
     * Enrich the provided draft identified by id. Implementations should
     * manage their own transactions where needed (e.g. REQUIRES_NEW).
     *
     * @param draftId id of the processing draft
     */
    void enrich(Long draftId);
}
