package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.config.policies.ScoringPolicy;
import java.util.Arrays;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class KahanSummationStabilityTest {

    @Test
    void kahan_accumulator_is_order_independent_within_tolerance() throws Exception {
        ScoringPolicy policy = new ScoringPolicy(false, 1.0, "FULL");
        ScoreCalculator calc = new ScoreCalculator(policy);

        Map<Long, Double> wSumsA = new HashMap<>();
        Map<Long, Double> wCompsA = new HashMap<>();

        Map<Long, Double> wSumsB = new HashMap<>();
        Map<Long, Double> wCompsB = new HashMap<>();

        Long markId = 1L;

        double[] values = new double[100];
        Arrays.fill(values, 1e-8);

        // Add values in forward order using KahanAccumulator directly
        for (double v : values) {
            KahanAccumulator.kahanScoresSum(wSumsA, wCompsA, markId, v);
        }

        // Add values in reverse order
        for (int i = values.length - 1; i >= 0; i--) {
            KahanAccumulator.kahanScoresSum(wSumsB, wCompsB, markId, values[i]);
        }

        double sumA = wSumsA.getOrDefault(markId, 0.0);
        double sumB = wSumsB.getOrDefault(markId, 0.0);

        assertEquals(sumA, sumB, 1e-12);
    }
}
