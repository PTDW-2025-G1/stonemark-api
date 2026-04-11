package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class KahanSummationStabilityTest {

    @Test
    void kahan_accumulator_is_order_independent_within_tolerance() {
        Map<Long, Double> wSums = new HashMap<>();
        Map<Long, Double> wComps = new HashMap<>();
        Long markId = 1L;

        // Sequence designed to cause cancellation in naive summation: large + small - large
        double large = 1e16;
        double small = 1.0;

        // Naive summation
        double naive = large + small - large;

        // Kahan accumulation
        KahanAccumulator.kahanScoresSum(wSums, wComps, markId, large);
        KahanAccumulator.kahanScoresSum(wSums, wComps, markId, small);
        KahanAccumulator.kahanScoresSum(wSums, wComps, markId, -large);

        // The corrected value is the running sum plus the compensation term.
        double kahan = wSums.getOrDefault(markId, 0.0) + wComps.getOrDefault(markId, 0.0);

        // The naive sum will typically be 0.0 due to cancellation/rounding.
        double tol = 1e-9;
        assertTrue(Math.abs(naive) < tol, "Naive summation in this scenario is expected to round the small term away");

        // Kahan accumulator should approximate the true mathematical sum (large + small - large = 1.0)
        assertEquals(1.0d, kahan, tol, "Kahan accumulator should recover the true mathematical sum within tolerance");
    }
}
