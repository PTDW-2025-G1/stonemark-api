package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;

import pt.estga.processing.config.policies.ScoringPolicy;
import pt.estga.processing.models.AggregationState;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EmptyInputSafetyTest {

    @Test
    void scoreCalculator_handles_empty_and_null_inputs() {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "FULL");
        ScoreCalculator calc = new ScoreCalculator(policy);

        AggregationState s1 = calc.compute(Map.of());
        assertTrue(s1.scores().isEmpty());
        assertTrue(s1.weightSums().isEmpty());
        assertEquals(0, s1.duplicates());
    }

    @Test
    void candidateGrouper_handles_empty_candidates_or_null_map() {
        CandidateGrouper grouper = new CandidateGrouper();
        var res1 = grouper.groupAndSort(List.of(), Map.of());
        assertTrue(res1.isEmpty());

        var res2 = grouper.groupAndSort(List.of(), null);
        assertTrue(res2.isEmpty());
    }
}
