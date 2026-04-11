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
}
