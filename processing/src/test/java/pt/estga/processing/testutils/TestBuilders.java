package pt.estga.processing.testutils;

import pt.estga.processing.models.CandidateEvidence;

import java.util.UUID;

public final class TestBuilders {

    private TestBuilders() {}

    public static CandidateEvidence candidate(UUID evidenceId, Long occurrenceId, double similarity) {
        return new CandidateEvidence(evidenceId, occurrenceId, similarity);
    }

    public static Long mark(Long id) {
        return id;
    }

    public static UUID uuid(String literal) {
        return UUID.fromString(literal);
    }
}
