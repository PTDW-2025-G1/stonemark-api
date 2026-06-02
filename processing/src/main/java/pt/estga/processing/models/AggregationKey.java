package pt.estga.processing.models;

import java.util.UUID;

/**
 * Aggregation-level key representing a unique (evidence, occurrence, mark)
 * contribution used for deduplication during scoring.
 */
public record AggregationKey(UUID evidenceId, Long occurrenceId, Long markId) {

    private static final Long MISSING = -1L;

    public static AggregationKey of(UUID evidenceId, Long occurrenceId, Long markId) {
        Long occ = occurrenceId == null ? MISSING : occurrenceId;
        return new AggregationKey(evidenceId, occ, markId);
    }

    public boolean isOccurrenceMissing() {
        return MISSING.equals(occurrenceId);
    }
}
