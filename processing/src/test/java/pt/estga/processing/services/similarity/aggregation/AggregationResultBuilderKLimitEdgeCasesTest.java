package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.AggregationState;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationResultBuilderKLimitEdgeCasesTest {

    @Test
    void k_edge_cases_behave_as_expected() {
        Map<Long, Double> scores = Map.of(1L, 0.9d, 2L, 0.4d, 3L, 0.2d);
        Map<Long, Double> weights = Map.of(1L, 1.0d, 2L, 0.5d, 3L, 0.25d);
        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,0);

        AggregationResultBuilder builder = new AggregationResultBuilder();
        Map<Long, Mark> marksById = Map.of(
                1L, Mark.builder().id(1L).build(),
                2L, Mark.builder().id(2L).build(),
                3L, Mark.builder().id(3L).build()
        );

        // k = 0 -> treated as no limit (full list)
        var r0 = builder.build(state, marksById, 0, 0);
        assertEquals(3, r0.topScores().size());

        // k > size -> full list
        var rBig = builder.build(state, marksById, 10, 0);
        assertEquals(3, rBig.topScores().size());

        // negative k -> treated as no limit
        var rNeg = builder.build(state, marksById, -1, 0);
        assertEquals(3, rNeg.topScores().size());
    }
}
