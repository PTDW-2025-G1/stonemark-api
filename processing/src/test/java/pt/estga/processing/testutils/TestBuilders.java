package pt.estga.processing.testutils;

import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.CandidateEvidence;

import java.util.UUID;

public final class TestBuilders {

    private TestBuilders() {}

    public static CandidateEvidence candidate(UUID evidenceId, Long occurrenceId, double similarity) {
        return new CandidateEvidence(evidenceId, occurrenceId, similarity);
    }

    public static Mark mark(Long id) {
        return Mark.builder().id(id).build();
    }

    public static UUID uuid(String literal) {
        // Accept either standard dashed UUID strings or compact hex literals used in tests.
        // Examples handled:
        // - "123e4567-e89b-12d3-a456-426614174000" -> UUID.fromString
        // - "0000000000000000" (16 hex chars) -> treat as least-significant 64 bits, msb=0
        // - "00112233445566778899aabbccddeeff" (32 hex chars) -> split into msb/lsb
        if (literal.contains("-")) {
            return UUID.fromString(literal);
        }
        String hex = literal.trim();
        if (hex.length() == 16) {
            long lsb = Long.parseUnsignedLong(hex, 16);
            return new UUID(0L, lsb);
        }
        if (hex.length() == 32) {
            long msb = Long.parseUnsignedLong(hex.substring(0, 16), 16);
            long lsb = Long.parseUnsignedLong(hex.substring(16), 16);
            return new UUID(msb, lsb);
        }
        // Fallback to the strict parser to keep existing behavior/error messages for unexpected input.
        return UUID.fromString(literal);
    }
}
