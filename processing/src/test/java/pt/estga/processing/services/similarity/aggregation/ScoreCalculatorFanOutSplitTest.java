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

public class ScoreCalculatorFanOutSplitTest {

    @Test
    void split_fanout_scales_contributions_across_marks() {
        // no rank weighting, no decay, SPLIT strategy
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "SPLIT");
        ScoreCalculator calc = new ScoreCalculator(policy);

        UUID e = TestBuilders.uuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        CandidateEvidence c1 = TestBuilders.candidate(e, 1L, 0.8);
        CandidateEvidence c2 = TestBuilders.candidate(e, 1L, 0.6);

        // both marks reference same evidence id -> fan-out = 2
        Map<Long, List<CandidateEvidence>> contributions = Map.of(
                10L, List.of(c1),
                20L, List.of(c2)
        );

        AggregationState state = calc.compute(contributions);

        // raw scores were scaled by 1/2
        Double raw10 = state.scores().get(10L);
        Double raw20 = state.scores().get(20L);
        Double w10 = state.weightSums().get(10L);
        Double w20 = state.weightSums().get(20L);
        assertNotNull(raw10);
        assertNotNull(raw20);

        // raw10 = 0.8 * 0.5 = 0.4
        assertEquals(0.4d, raw10, 1e-9);
        // raw20 = 0.6 * 0.5 = 0.3
        assertEquals(0.3d, raw20, 1e-9);
        // weight sum for each mark: perMarkMultiplier(1.0) * scale(0.5) = 0.5
        assertEquals(0.5d, w10, 1e-9);
        assertEquals(0.5d, w20, 1e-9);

        // When converted to confidence by AggregationResultBuilder, confidence = raw / weight -> original similarity
        double conf10 = raw10 / w10;
        double conf20 = raw20 / w20;
        assertEquals(0.8d, conf10, 1e-6);
        assertEquals(0.6d, conf20, 1e-6);

        // check fanOutContributionCount increment: two contributions (one per mark) observed for the same evidence
        assertEquals(2, state.fanOutContributionCount());
    }
}
