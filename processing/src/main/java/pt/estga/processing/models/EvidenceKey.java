package pt.estga.processing.models;

import java.util.UUID;

/**
 * Typed key representing a unique evidence->mark contribution.
 * <p>
 * Use a distinct typed record (evidenceId, markId, occurrenceId) instead of
 * string concatenation to avoid accidental collisions and make deduplication
 * explicit and type-safe. occurrenceId may be null when an evidence row does
 * not provide one.
 */
public record EvidenceKey(UUID evidenceId, Long markId, Long occurrenceId) {

	private static final Long MISSING_OCCURRENCE = -1L;

	/**
	 * Factory that normalizes nullable occurrenceId to a sentinel value to avoid
	 * inconsistent deduplication when some sources provide null and others provide
	 * explicit occurrence ids.
	 */
	public static EvidenceKey of(UUID evidenceId, Long markId, Long occurrenceId) {
		Long occ = occurrenceId == null ? MISSING_OCCURRENCE : occurrenceId;
		return new EvidenceKey(evidenceId, markId, occ);
	}

	/**
	 * Return true if occurrence was originally missing (normalized sentinel).
	 */
	public boolean isOccurrenceMissing() {
		return MISSING_OCCURRENCE.equals(occurrenceId);
	}
}
