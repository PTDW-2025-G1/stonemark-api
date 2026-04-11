package pt.estga.processing.services.similarity.aggregation;

import java.util.Map;

public final class KahanAccumulator {
    private KahanAccumulator() {}

    public static void kahanScoresSum(Map<Long, Double> sums, Map<Long, Double> comps, Long markId, double value) {
        double w = sums.getOrDefault(markId, 0.0);
        double wc = comps.getOrDefault(markId, 0.0);
        double wy = value - wc;
        double wt = w + wy;
        wc = (wt - w) - wy;
        w = wt;
        sums.put(markId, w);
        comps.put(markId, wc);
    }
}
