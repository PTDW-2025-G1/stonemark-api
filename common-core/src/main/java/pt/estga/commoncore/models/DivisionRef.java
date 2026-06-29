package pt.estga.commoncore.models;

/**
 * Reference to an administrative division resolved from the external geo service.
 * The code is the canonical identifier (e.g. CAOP DICOFRE, leading zeros preserved);
 * the name is the human-readable label and may be null when not resolved.
 */
public record DivisionRef(String code, String name) {}
