package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.CandidateEvidence;
import pt.estga.processing.testutils.TestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FanOutStrategyComparisonTest {

    @Test
    void split_vs_full_fanout_results_in_different_scaling() {
        UUID e = TestBuilders.uuid("cccccccc-0000-0000-0000-00000000000c");
        CandidateEvidence ce = TestBuilders.candidate(e, 1L, 0.8);

        // Build contributions: evidence appears for two marks
        Map<Long, List<CandidateEvidence>> contributions = Map.of(
                10L, List.of(ce),
                20L, List.of(ce)
        );

        ScoringPolicy splitPolicy = new ScoringPolicy(false, 1.0, "SPLIT");
        ScoringPolicy fullPolicy = new ScoringPolicy(false, 1.0, "FULL");

        ScoreCalculator calcSplit = new ScoreCalculator(splitPolicy);
        ScoreCalculator calcFull = new ScoreCalculator(fullPolicy);

        var sSplit = calcSplit.compute(contributions);
        var sFull = calcFull.compute(contributions);

        // In SPLIT, each mark gets similarity * 1/2 = 0.4 raw; in FULL each gets full 0.8
        assertEquals(0.4d, sSplit.scores().get(10L), 1e-9);
        assertEquals(0.4d, sSplit.scores().get(20L), 1e-9);

        assertEquals(0.8d, sFull.scores().get(10L), 1e-9);
        assertEquals(0.8d, sFull.scores().get(20L), 1e-9);
    }
}
