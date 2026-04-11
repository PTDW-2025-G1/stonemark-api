package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ScoreCalculatorDeduplicationTest {

    @Test
    void deduplicates_same_evidence_occurrence_mark_and_counts_duplicates() {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "FULL");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e1 = TestBuilders.uuid("11111111-1111-1111-1111-111111111111");
        UUID e2 = TestBuilders.uuid("22222222-2222-2222-2222-222222222222");

        // mark 42 has two entries for same evidence e1, occurrence 7L -> duplicate; keep highest similarity
        CandidateEvidence a = TestBuilders.candidate(e1, 7L, 0.2);
        CandidateEvidence b = TestBuilders.candidate(e1, 7L, 0.7);
        CandidateEvidence c = TestBuilders.candidate(e2, null, 0.5);

        Map<Long, List<CandidateEvidence>> contributions = Map.of(42L, List.of(a, b, c));

        AggregationState state = calc.compute(contributions);

        assertEquals(1, state.duplicates());

        Double raw = state.scores().get(42L);
        Double weight = state.weightSums().get(42L);
        assertNotNull(raw);
        assertNotNull(weight);

        // Exact expected behaviour: duplicate removed, highest similarity chosen, totals as below
        assertEquals(1, state.duplicates());
        // expected raw = 0.7 (best for e1) + 0.5 (e2) = 1.2
        assertEquals(1.2d, state.scores().get(42L), 1e-9);
        // expected weight = 1.0 + 1.0 = 2.0
        assertEquals(2.0d, state.weightSums().get(42L), 1e-9);
    }
}
