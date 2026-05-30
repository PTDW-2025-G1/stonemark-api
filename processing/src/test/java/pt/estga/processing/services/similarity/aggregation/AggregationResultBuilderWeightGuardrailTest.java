package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.models.AggregationState;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationResultBuilderWeightGuardrailTest {

    @Test
    void guards_against_zero_or_near_zero_weights_and_returns_zero_confidence() {
        Map<Long, Double> scores = Map.of(1L, 0.5d);
        Map<Long, Double> weightsZero = Map.of(1L, 0.0d);
        AggregationState stateZero = new AggregationState(scores, weightsZero, 0, 0, 0, 0, 0);

        AggregationResultBuilder builder = new AggregationResultBuilder();

        Set<Long> validMarkIds = Set.of(1L);

        var resZero = builder.build(stateZero, validMarkIds, 10, 0);

        assertNotNull(resZero);
        assertFalse(resZero.topScores().isEmpty());
        assertEquals(0.0d, resZero.topScores().getFirst().confidence(), 1e-12);

        Map<Long, Double> weightsTiny = Map.of(1L, 1e-20);
        AggregationState stateTiny = new AggregationState(scores, weightsTiny, 0, 0, 0, 0, 0);
        var resTiny = builder.build(stateTiny, validMarkIds, 10, 0);
        assertNotNull(resTiny);
        assertEquals(0.0d, resTiny.topScores().getFirst().confidence(), 1e-12);
    }
}
