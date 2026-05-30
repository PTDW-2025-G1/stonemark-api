package pt.estga.shared.utils;

/**
 * Utility helpers for working with vector data representations.
 */
public final class VectorUtils {

    private VectorUtils() {
        // utility class
    }

    /**
     * Convert a primitive float array to a Postgres/pgvector literal string.
     * Example: [0.1,0.2,0.3]
     *
     * @param vec input vector
     * @return literal string suitable for use in native queries
     */
    public static String toVectorLiteral(float[] vec) {
        if (vec == null || vec.length == 0) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Compute cosine similarity between two vectors. Returns null when inputs are invalid
     * (different lengths or zero-norm) to allow callers to skip such entries gracefully.
     */
    public static Double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) return null;
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0.0 || nb == 0.0) return null;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
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
