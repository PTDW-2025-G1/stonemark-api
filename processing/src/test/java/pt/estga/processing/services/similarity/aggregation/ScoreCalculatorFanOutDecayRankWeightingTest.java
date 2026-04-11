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

public class ScoreCalculatorFanOutDecayRankWeightingTest {

    @Test
    void combined_fanout_decay_and_rank_weighting_behaves_as_expected() {
        // use rank weighting, decay=0.5, SPLIT strategy
        ScoringPolicy policy = new ScoringPolicy(true, 0.5, "SPLIT");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e = TestBuilders.uuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // mark A and B both reference same evidence id (fan-out = 2)
        // Each mark has three contributions (different similarities to exercise rank weighting and decay)
        CandidateEvidence a0 = TestBuilders.candidate(e, 1L, 0.9);
        CandidateEvidence a1 = TestBuilders.candidate(e, 2L, 0.6);
        CandidateEvidence a2 = TestBuilders.candidate(e, 3L, 0.3);

        CandidateEvidence b0 = TestBuilders.candidate(e, 4L, 0.8);
        CandidateEvidence b1 = TestBuilders.candidate(e, 5L, 0.5);
        CandidateEvidence b2 = TestBuilders.candidate(e, 6L, 0.4);

        Map<Long, List<CandidateEvidence>> contributions = Map.of(
                100L, List.of(a0, a1, a2),
                200L, List.of(b0, b1, b2)
        );

        AggregationState state = calc.compute(contributions);

        // Expected calculations (manual):
        // For each mark, contributions scaled by fan-out=2 (scale=0.5)
        // rankScore = 1/(1+i), perMarkMultiplier = decay^i where decay=0.5

        // Mark 100 raw expected (computed manually): 0.5375
        double expectedRaw100 = 0.5375d;
        double expectedWeight = 0.5 + 0.25 + 0.125; // 0.875

        // Mark 200 raw expected: 0.4791666667
        double expectedRaw200 = 0.4791666666666667d;

        assertEquals(expectedRaw100, state.scores().get(100L), 1e-9);
        assertEquals(expectedRaw200, state.scores().get(200L), 1e-9);

        assertEquals(expectedWeight, state.weightSums().get(100L), 1e-12);
        assertEquals(expectedWeight, state.weightSums().get(200L), 1e-12);

        // Confidences (raw/weight) should differ deterministically
        double conf100 = state.scores().get(100L) / state.weightSums().get(100L);
        double conf200 = state.scores().get(200L) / state.weightSums().get(200L);
        assertTrue(conf100 > conf200);
    }
}
