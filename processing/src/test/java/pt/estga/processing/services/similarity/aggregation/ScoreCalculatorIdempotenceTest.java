package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ScoreCalculatorIdempotenceTest {

    @Test
    void compute_is_idempotent_across_many_runs() {
        ScoringPolicy policy = new ScoringPolicy(true, 0.5, "SPLIT");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e1 = TestBuilders.uuid("aaaaaaaa-0000-0000-0000-000000000001");
        UUID e2 = TestBuilders.uuid("bbbbbbbb-0000-0000-0000-000000000002");

        Map<Long, List<CandidateEvidence>> contributions = new HashMap<>();
        contributions.put(1L, new ArrayList<>(List.of(
                TestBuilders.candidate(e1, 1L, 0.9),
                TestBuilders.candidate(e2, 2L, 0.5)
        )));

        // Keep a defensive copy of input for mutation detection
        Map<Long, List<CandidateEvidence>> originalCopy = new HashMap<>();
        contributions.forEach((k,v) -> originalCopy.put(k, new ArrayList<>(v)));

        AggregationState baseline = calc.compute(contributions);

        for (int i = 0; i < 100; i++) {
            AggregationState s = calc.compute(contributions);
            // maps keys must match
            assertEquals(baseline.scores().keySet(), s.scores().keySet());
            assertEquals(baseline.weightSums().keySet(), s.weightSums().keySet());
            // numeric equality within tolerance
            for (Long k : baseline.scores().keySet()) {
                assertEquals(baseline.scores().get(k), s.scores().get(k), 1e-9);
                assertEquals(baseline.weightSums().get(k), s.weightSums().get(k), 1e-12);
            }
            assertEquals(baseline.duplicates(), s.duplicates());
            assertEquals(baseline.fanOutContributionCount(), s.fanOutContributionCount());
        }

        // Ensure input was not mutated
        assertEquals(originalCopy.keySet(), contributions.keySet());
        for (Long k : originalCopy.keySet()) {
            assertEquals(originalCopy.get(k).size(), contributions.get(k).size());
        }
    }
}
