package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.models.AggregationState;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationResultBuilderKLimitEdgeCasesTest {

    @Test
    void k_edge_cases_behave_as_expected() {
        Map<Long, Double> scores = Map.of(1L, 0.9d, 2L, 0.4d, 3L, 0.2d);
        Map<Long, Double> weights = Map.of(1L, 1.0d, 2L, 0.5d, 3L, 0.25d);
        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,0);

        AggregationResultBuilder builder = new AggregationResultBuilder();
        Set<Long> validMarkIds = Set.of(1L, 2L, 3L);

        var r0 = builder.build(state, validMarkIds, 0, 0);
        assertEquals(3, r0.topScores().size());

        var rBig = builder.build(state, validMarkIds, 10, 0);
        assertEquals(3, rBig.topScores().size());

        var rNeg = builder.build(state, validMarkIds, -1, 0);
        assertEquals(3, rNeg.topScores().size());
    }
}
