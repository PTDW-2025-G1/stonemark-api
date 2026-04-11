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
        return UUID.fromString(literal);
    }
}
