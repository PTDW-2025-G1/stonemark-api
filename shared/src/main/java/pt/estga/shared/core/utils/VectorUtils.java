package pt.estga.shared.core.utils;

/**
 * Utility helpers for working with vector data representations.
 */
public final class VectorUtils {

    private VectorUtils() {
        // utility class
    }

    /**
     * Normalize a float[] vector in-place and return a new float[] normalized to unit length.
     * Returns null when input is null/empty or has zero norm.
     */
    public static float[] normalize(float[] v) {
        if (v == null || v.length == 0) return null;
        double sumSq = 0.0;
        for (float x : v) sumSq += (double) x * x;
        if (sumSq == 0.0) return null;
        double norm = Math.sqrt(sumSq);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }

    /**
     * Compute L2 norm of a float[] vector. Returns NaN for null or empty inputs.
     */
    public static double l2Norm(float[] v) {
        if (v == null || v.length == 0) return Double.NaN;
        double sumSq = 0.0;
        for (float x : v) sumSq += (double) x * x;
        return Math.sqrt(sumSq);
    }
}
