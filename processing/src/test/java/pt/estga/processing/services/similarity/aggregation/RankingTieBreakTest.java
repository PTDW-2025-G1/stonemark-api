package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.models.AggregationState;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RankingTieBreakTest {

    @Test
    void equal_confidence_marks_sorted_by_markId_asc() {
        Map<Long, Double> scores = Map.of(2L, 1.0d, 1L, 0.5d);
        Map<Long, Double> weights = Map.of(2L, 2.0d, 1L, 1.0d);
        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,0);

        AggregationResultBuilder builder = new AggregationResultBuilder();
        Set<Long> validMarkIds = Set.of(2L, 1L);

        var res = builder.build(state, validMarkIds, 10, 0);

        assertEquals(2, res.topScores().size());
        assertEquals(1L, res.topScores().get(0).markId());
        assertEquals(2L, res.topScores().get(1).markId());
    }
}
