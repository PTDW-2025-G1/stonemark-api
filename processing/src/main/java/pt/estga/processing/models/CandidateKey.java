package pt.estga.processing.models;

import java.util.UUID;

/**
 * Consolidated candidate key used for deduplication and uniqueness checks.
 * A null {@code occurrenceId} means the occurrence is not yet known.
 */
public record CandidateKey(UUID evidenceId, UUID occurrenceId, UUID markId) {

    /**
     * Sanitizer-level factory: markId is unknown at sanitization time.
     */
    public static CandidateKey of(UUID evidenceId, UUID occurrenceId) {
        return new CandidateKey(evidenceId, occurrenceId, null);
    }

    /**
     * Aggregation-level factory: includes markId to distinguish contributions targeted at different marks.
     */
    public static CandidateKey of(UUID evidenceId, UUID occurrenceId, UUID markId) {
        return new CandidateKey(evidenceId, occurrenceId, markId);
    }

    public boolean isOccurrenceMissing() {
        return occurrenceId == null;
    }

    public boolean hasMark() {
        return markId != null;
    }
}
