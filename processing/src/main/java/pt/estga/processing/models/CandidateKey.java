package pt.estga.processing.models;

import java.util.UUID;

/**
 * Lightweight key representing a candidate evidence instance for uniqueness checks
 * at the sanitization boundary. occurrenceId is normalized to -1L when missing.
 */
public record CandidateKey(UUID evidenceId, Long occurrenceId) {

    private static final Long MISSING = -1L;

    public static CandidateKey of(UUID evidenceId, Long occurrenceId) {
        Long occ = occurrenceId == null ? MISSING : occurrenceId;
        return new CandidateKey(evidenceId, occ);
    }

    public boolean isOccurrenceMissing() {
        return MISSING.equals(occurrenceId);
    }
}
