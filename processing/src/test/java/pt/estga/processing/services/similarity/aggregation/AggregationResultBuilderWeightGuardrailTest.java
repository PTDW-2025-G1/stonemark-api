package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.AggregationState;
import pt.estga.processing.models.AggregationResult;
import pt.estga.processing.services.similarity.aggregation.AggregationResultBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationResultBuilderWeightGuardrailTest {

    @Test
    void guards_against_zero_or_near_zero_weights_and_returns_zero_confidence() {
        // Prepare a state with a non-empty score and a zero weight
        Map<Long, Double> scores = Map.of(1L, 0.5d);
        Map<Long, Double> weights = Map.of(1L, 0.0d);
        AggregationState state = new AggregationState(scores, weights, 0, 0, 0, 0, 0);

        AggregationResultBuilder builder = new AggregationResultBuilder();

        Mark m = Mark.builder().id(1L).build();
        var marksById = Map.of(1L, m);

        AggregationResult res = builder.build(state, marksById, 10, 0);

        assertNotNull(res);
        assertFalse(res.topScores().isEmpty());
        // confidence should be set to 0.0 when weight <= MIN_WEIGHT
        assertEquals(0.0d, res.topScores().getFirst().confidence(), 1e-12);
    }
}
