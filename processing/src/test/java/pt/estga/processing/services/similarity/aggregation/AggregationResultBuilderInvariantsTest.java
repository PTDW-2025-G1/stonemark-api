package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.models.AggregationState;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

        Set<Long> validMarkIds = Set.of(1L);

        var res = builder.build(state, validMarkIds, 10, 0);

        assertTrue(res.topScores().stream().anyMatch(s -> s.markId() == 1L));
        assertFalse(res.topScores().stream().anyMatch(s -> s.markId() == 3L));

        assertTrue(res.weightSums().containsKey(3L));
    }

    @Test
    void sorting_is_deterministic_even_when_input_map_is_shuffled() {
        Map<Long, Double> scores = Map.of(5L, 0.5d, 2L, 0.6d, 9L, 0.4d);
        Map<Long, Double> weights = Map.of(5L, 1.0d, 2L, 1.0d, 9L, 1.0d);
        AggregationState state = new AggregationState(scores, weights, 0,0,0,0,0);

        AggregationResultBuilder builder = new AggregationResultBuilder();

        Set<Long> validMarkIds = new LinkedHashSet<>();
        validMarkIds.add(9L);
        validMarkIds.add(2L);
        validMarkIds.add(5L);

        var res = builder.build(state, validMarkIds, 10, 0);

        assertEquals(2L, res.topScores().get(0).markId());
        assertEquals(5L, res.topScores().get(1).markId());
        assertEquals(9L, res.topScores().get(2).markId());
    }
}
