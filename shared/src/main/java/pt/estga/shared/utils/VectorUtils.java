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
}
