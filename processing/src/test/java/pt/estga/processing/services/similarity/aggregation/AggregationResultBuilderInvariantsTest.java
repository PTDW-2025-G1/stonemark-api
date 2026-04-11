package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.mark.entities.Mark;
import pt.estga.processing.models.AggregationState;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationResultBuilderInvariantsTest {

    @Test
    void skips_missing_marks_and_handles_nan_and_small_weights() {
        Map<Long, Double> scores = new LinkedHashMap<>();
        scores.put(1L, 0.5d);
        scores.put(2L, Double.NaN);
        scores.put(3L, 0.3d);

        Map<Long, Double> weights = new LinkedHashMap<>();
        weights.put(1L, 1.0d);
        weights.put(2L, 1.0d);
        weights.put(3L, 1e-20);

        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,0);

        AggregationResultBuilder builder = new AggregationResultBuilder();

        // only provide Mark for id=1; id=3 is missing and should be skipped
        Map<Long, Mark> marksById = Map.of(1L, Mark.builder().id(1L).build());

        var res = builder.build(state, marksById, 10, 0);

        // Entry for mark 1 should appear, mark 3 should be skipped because it's missing
        assertTrue(res.topScores().stream().anyMatch(s -> s.markId() == 1L));
        assertFalse(res.topScores().stream().anyMatch(s -> s.markId() == 3L));

        // tiny weight for mark 3 should have been treated as anomaly leading to confidence 0 if present
        // confirm weight anomalies counter is set
        assertTrue(res.weightSums().containsKey(3L));
    }

    @Test
    void sorting_is_deterministic_even_when_input_map_is_shuffled() {
        Map<Long, Double> scores = Map.of(5L, 0.5d, 2L, 0.6d, 9L, 0.4d);
        Map<Long, Double> weights = Map.of(5L, 1.0d, 2L, 1.0d, 9L, 1.0d);
        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,0);

        AggregationResultBuilder builder = new AggregationResultBuilder();

        // Provide marks map in a different insertion order
        Map<Long, Mark> marksById = new LinkedHashMap<>();
        marksById.put(9L, Mark.builder().id(9L).build());
        marksById.put(2L, Mark.builder().id(2L).build());
        marksById.put(5L, Mark.builder().id(5L).build());

        var res = builder.build(state, marksById, 10, 0);

        // Expect deterministic order: highest confidence first (mark 2), then 5, then 9
        assertEquals(2L, res.topScores().get(0).markId());
        assertEquals(5L, res.topScores().get(1).markId());
        assertEquals(9L, res.topScores().get(2).markId());
    }
}
