package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.models.AggregationState;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationResultBuilderKLimitTest {

    @Test
    void k_limit_truncates_top_scores_but_preserves_raw_maps() {
        Map<Long, Double> scores = Map.of(1L, 0.9d, 2L, 0.4d, 3L, 0.2d);
        Map<Long, Double> weights = Map.of(1L, 1.0d, 2L, 0.5d, 3L, 0.25d);
        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,0);

        AggregationResultBuilder builder = new AggregationResultBuilder();
        Set<Long> validMarkIds = Set.of(1L, 2L, 3L);

        var res = builder.build(state, validMarkIds, 2, 0);

        assertEquals(2, res.topScores().size());
        assertTrue(res.topScores().get(0).confidence() >= res.topScores().get(1).confidence());

        assertEquals(3, res.rawScores().size());
        assertEquals(3, res.weightSums().size());
    }
}
