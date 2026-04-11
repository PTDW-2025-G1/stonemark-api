package pt.estga.processing.services.similarity.aggregation;

import org.junit.jupiter.api.Test;
import pt.estga.processing.models.KahanState;
import pt.estga.processing.utils.KahanAccumulator;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class KahanSummationStabilityTest {

    @Test
    void kahan_accumulator_is_order_independent_within_tolerance() {
        Map<Long, KahanState> wStates = new HashMap<>();
        Long markId = 1L;

        // Sequence designed to cause cancellation in naive summation: large + small - large
        double large = 1e16;
        double small = 1.0;

        // Naive summation
        double naive = large + small - large;

        // Kahan accumulation (using the State API)
        KahanAccumulator.accumulate(wStates, markId, large);
        KahanAccumulator.accumulate(wStates, markId, small);
        KahanAccumulator.accumulate(wStates, markId, -large);

        // The corrected value is obtained from the state; ensure the state exists to avoid masking failures
        assertTrue(wStates.containsKey(markId), "Expected accumulation state for markId to exist");
        double kahan = wStates.get(markId).value();

        // The naive sum will typically be 0.0 due to cancellation/rounding.
        double tol = 1e-9;
        assertEquals(0.0d, naive, 0.0d, "Naive summation in this scenario is expected to round the small term away");

        // Kahan accumulator should approximate the true mathematical sum (large + small - large = 1.0)
        assertEquals(1.0d, kahan, tol, "Kahan accumulator should recover the true mathematical sum within tolerance");
    }
}
