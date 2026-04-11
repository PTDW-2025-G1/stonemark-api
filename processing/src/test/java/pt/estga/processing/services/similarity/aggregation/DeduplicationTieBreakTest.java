package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DeduplicationTieBreakTest {

    @Test
    void ties_are_resolved_by_first_seen_behavior() {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "FULL");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e1 = TestBuilders.uuid("10101010-1010-1010-1010-101010101010");

        // two entries with equal similarity for same evidence and occurrence
        CandidateEvidence a = TestBuilders.candidate(e1, 7L, 0.5);
        CandidateEvidence b = TestBuilders.candidate(e1, 7L, 0.5);

        Map<Long, List<CandidateEvidence>> contributions = Map.of(42L, List.of(a, b));

        var state = calc.compute(contributions);

        // Duplicates counter should increment by 1 (the second entry is duplicate)
        assertEquals(1, state.duplicates());

        // Since similarities tie, first-seen policy keeps 'a' and raw score equals 0.5
        assertEquals(0.5d, state.scores().get(42L), 1e-9);
    }
}
