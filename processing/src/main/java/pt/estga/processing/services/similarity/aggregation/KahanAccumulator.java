package pt.estga.processing.services.similarity.aggregation;

import java.util.Map;

public final class KahanAccumulator {
    private KahanAccumulator() {}

    public static void kahanScoresSum(Map<Long, Double> sums, Map<Long, Double> comps, Long markId, double value) {
        // Use Neumaier's variant of compensated summation which is generally
        // more robust than the basic Kahan form for sequences where a large
        // partial sum may dominate incoming values. We keep a running sum
        // and a separate compensation term; callers can combine both to
        // obtain the corrected total.
        double sum = sums.getOrDefault(markId, 0.0);
        double comp = comps.getOrDefault(markId, 0.0);

        double t = sum + value;
        if (Math.abs(sum) >= Math.abs(value)) {
            comp += (sum - t) + value;
        } else {
            comp += (value - t) + sum;
        }
        sum = t;

        sums.put(markId, sum);
        comps.put(markId, comp);
    }
}
