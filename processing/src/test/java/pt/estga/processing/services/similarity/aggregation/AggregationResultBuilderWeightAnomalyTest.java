package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.models.AggregationState;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationResultBuilderWeightAnomalyTest {

    @Test
    void zero_and_tiny_and_nan_weights_behave_as_contract() {
        Map<Long, Double> scores = Map.of(1L, 0.5d, 2L, 0.5d, 3L, 0.5d);
        Map<Long, Double> weights = Map.of(1L, 0.0d, 2L, 1e-13, 3L, Double.NaN);
        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,2);

        AggregationResultBuilder builder = new AggregationResultBuilder();
        Set<Long> validMarkIds = Set.of(1L, 2L, 3L);

        var res = builder.build(state, validMarkIds, 10, 0);

        assertEquals(0.0d, res.topScores().stream().filter(s -> s.markId() == 1L).findFirst().map(pt.estga.processing.models.MarkScore::confidence).orElse(-1d), 1e-12);
        assertEquals(0.0d, res.topScores().stream().filter(s -> s.markId() == 2L).findFirst().map(pt.estga.processing.models.MarkScore::confidence).orElse(-1d), 1e-12);

        assertTrue(res.weightSums().containsKey(3L));
    }
}
