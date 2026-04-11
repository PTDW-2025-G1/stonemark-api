package pt.estga.processing.models;

import java.util.UUID;

/**
 * Typed key representing a unique evidence->mark contribution with an occurrence token.
 * Using a record instead of string concatenation avoids accidental collisions and
 * makes deduplication explicit and type-safe.
 */
public record EvidenceKey(UUID evidenceId, Long markId, String occurrenceToken) {
}
