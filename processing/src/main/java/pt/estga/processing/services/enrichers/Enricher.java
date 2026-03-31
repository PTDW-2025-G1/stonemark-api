package pt.estga.processing.services.enrichers;

/**
 * Contract for an enrichment unit that can enrich a submission by id.
 */
public interface Enricher {
    /**
     * Enrich the provided submission identified by id. Implementations should
     * manage their own transactions where needed (e.g. REQUIRES_NEW).
     *
     * @param submissionId id of the submission
     */
    void enrich(Long submissionId);
}
