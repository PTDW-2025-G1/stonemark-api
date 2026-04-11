package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class NullSafetyTest {

    @Test
    void scoreCalculator_ignores_null_candidates_and_null_lists() {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "FULL");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e1 = TestBuilders.uuid("eeeeeeee-0000-0000-0000-00000000000e");
        CandidateEvidence good = TestBuilders.candidate(e1, 1L, 0.6);

        // mark 10 has a null list (should be tolerated) and mark 20 has a list with a null element
        Map<Long, List<CandidateEvidence>> contributions = new java.util.HashMap<>();
        contributions.put(10L, null);
        java.util.List<CandidateEvidence> listWithNull = new java.util.ArrayList<>();
        listWithNull.add(null);
        listWithNull.add(good);
        contributions.put(20L, listWithNull);

        var state = calc.compute(contributions);

        // Should process only the non-null candidate
        assertTrue(state.scores().containsKey(20L));
        assertFalse(state.scores().containsKey(10L));
        assertEquals(0, state.duplicates());
    }
}
