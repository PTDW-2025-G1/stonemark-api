package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.AggregationState;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RankingTieBreakTest {

    @Test
    void equal_confidence_marks_sorted_by_markId_asc() {
        // Two marks with identical score/weight -> same confidence
        Map<Long, Double> scores = Map.of(2L, 1.0d, 1L, 0.5d);
        Map<Long, Double> weights = Map.of(2L, 2.0d, 1L, 1.0d); // both confidences = 0.5
        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,0);

        AggregationResultBuilder builder = new AggregationResultBuilder();
        Map<Long, Mark> marksById = Map.of(
                2L, Mark.builder().id(2L).build(),
                1L, Mark.builder().id(1L).build()
        );

        var res = builder.build(state, marksById, 10, 0);

        // both present
        assertEquals(2, res.topScores().size());
        // tie-breaker: markId ascending => 1 then 2
        assertEquals(1L, res.topScores().get(0).markId());
        assertEquals(2L, res.topScores().get(1).markId());
    }
}
