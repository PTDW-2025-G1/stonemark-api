package pt.estga.processing.models;

import java.util.UUID;

/**
 * Key used at the sanitization boundary representing an evidence occurrence.
 */
public record SanitizationKey(UUID evidenceId, Long occurrenceId) {

    private static final Long MISSING = -1L;

    public static SanitizationKey of(UUID evidenceId, Long occurrenceId) {
        Long occ = occurrenceId == null ? MISSING : occurrenceId;
        return new SanitizationKey(evidenceId, occ);
    }

    public boolean isOccurrenceMissing() {
        return MISSING.equals(occurrenceId);
    }
}
