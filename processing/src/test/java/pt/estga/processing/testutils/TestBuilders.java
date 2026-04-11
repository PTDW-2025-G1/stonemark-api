package pt.estga.processing.testutils;

import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.CandidateEvidence;

import java.nio.charset.StandardCharsets;
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
        // For compact literals use a deterministic name-based UUID to avoid
        // producing low-entropy / MSB-zero UUIDs that can collide in tests.
        if (hex.length() == 16 || hex.length() == 32) {
            return UUID.nameUUIDFromBytes(("evidence-" + hex).getBytes(StandardCharsets.UTF_8));
        }
        // Fallback to the strict parser to keep existing behavior/error messages for unexpected input.
        return UUID.fromString(literal);
    }
}
