package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.AggregationState;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationResultBuilderWeightAnomalyTest {

    @Test
    void zero_and_tiny_and_nan_weights_behave_as_contract() {
        Map<Long, Double> scores = Map.of(1L, 0.5d, 2L, 0.5d, 3L, 0.5d);
        Map<Long, Double> weights = Map.of(1L, 0.0d, 2L, 1e-13, 3L, Double.NaN);
        // Pass weightAnomalies value as if computed upstream (for tests we set it to 2 for 1 and 2)
        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,2);

        AggregationResultBuilder builder = new AggregationResultBuilder();
        Map<Long, Mark> marksById = Map.of(
                1L, Mark.builder().id(1L).build(),
                2L, Mark.builder().id(2L).build(),
                3L, Mark.builder().id(3L).build()
        );

        // For zero and tiny weights, confidences should be 0. For NaN weight, builder may produce an exception
        var res = builder.build(state, marksById, 10, 0);

        assertEquals(0.0d, res.topScores().stream().filter(s -> s.markId() == 1L).findFirst().map(pt.estga.processing.models.MarkScore::confidence).orElse(-1d), 1e-12);
        assertEquals(0.0d, res.topScores().stream().filter(s -> s.markId() == 2L).findFirst().map(pt.estga.processing.models.MarkScore::confidence).orElse(-1d), 1e-12);

        // mark 3 had NaN weight; builder should include weightSums entry and not crash; confidence may be absent if mark was skipped
        assertTrue(res.weightSums().containsKey(3L));
    }
}
