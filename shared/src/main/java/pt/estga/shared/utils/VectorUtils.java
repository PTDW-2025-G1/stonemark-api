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
     * Normalize a float[] vector to unit-length and return a double[] copy.
     * Returns null when input is null/empty or has zero norm.
     */
    public static double[] normalize(float[] v) {
        if (v == null || v.length == 0) return null;
        double sumSq = 0.0;
        for (float x : v) sumSq += (double) x * x;
        if (sumSq == 0.0) return null;
        double norm = Math.sqrt(sumSq);
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] / norm;
        return out;
    }

    /**
     * Normalize a double[] vector to unit-length and return a double[] copy.
     * Returns null when input is null/empty or has zero norm.
     */
    public static double[] normalize(double[] v) {
        if (v == null || v.length == 0) return null;
        double sumSq = 0.0;
        for (double x : v) sumSq += x * x;
        if (sumSq == 0.0) return null;
        double norm = Math.sqrt(sumSq);
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] / norm;
        return out;
    }

    /**
     * Cosine similarity for already-normalized double[] vectors. Returns null when
     * inputs are invalid (different lengths or null).
     */
    public static Double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null) return null;
        if (a.length != b.length) return null;
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot;
    }
}
