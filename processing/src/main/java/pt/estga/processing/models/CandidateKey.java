package pt.estga.processing.models;

import java.util.UUID;

/**
 * Consolidated candidate key used for deduplication and uniqueness checks.
 */
public record CandidateKey(UUID evidenceId, Long occurrenceId, Long markId) {

    private static final Long MISSING = -1L;

    /**
     * Sanitizer-level factory: markId is unknown at sanitization time.
     */
    public static CandidateKey of(UUID evidenceId, Long occurrenceId) {
        Long occ = occurrenceId == null ? MISSING : occurrenceId;
        return new CandidateKey(evidenceId, occ, null);
    }

    /**
     * Aggregation-level factory: includes markId to distinguish contributions targeted at different marks.
     */
    public static CandidateKey of(UUID evidenceId, Long occurrenceId, Long markId) {
        Long occ = occurrenceId == null ? MISSING : occurrenceId;
        return new CandidateKey(evidenceId, occ, markId);
    }

    public boolean isOccurrenceMissing() {
        return MISSING.equals(occurrenceId);
    }

    public boolean hasMark() {
        return markId != null;
    }
}
