package pt.estga.processing.utils;

import pt.estga.processing.models.KahanState;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Compensated summation utilities. Replaces the previous two-map approach
 * (separate sums and compensations) with a single state object per key.
 * <p>
 * Use {@link KahanState#add(double)} to accumulate values and {@link KahanState#value()}
 * to read the corrected total.
 */
public final class KahanAccumulator {
    private KahanAccumulator() {}

    /**
     * Accumulate `value` into the state map for `key`, creating the state if
     * absent.
     */
    public static void accumulate(Map<Long, KahanState> states, Long key, double value) {
        Objects.requireNonNull(states, "states map");
        KahanState s = states.computeIfAbsent(key, _ -> new KahanState());
        s.add(value);
    }

    /**
     * Produce a corrected map of totals (sum + compensation) from a state map.
     * The returned map is a TreeMap to give deterministic key ordering.
     */
    public static Map<Long, Double> toCorrectedMap(Map<Long, KahanState> states) {
        Map<Long, Double> corrected = new TreeMap<>();
        if (states == null) return corrected;
        for (Map.Entry<Long, KahanState> e : states.entrySet()) corrected.put(e.getKey(), e.getValue().value());
        return corrected;
    }
}
