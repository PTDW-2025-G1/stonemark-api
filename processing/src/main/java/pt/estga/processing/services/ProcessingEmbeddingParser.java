package pt.estga.processing.services;

/**
 * Parses the pgvector text representation (for example {@code [0.1,0.2,...]})
 * back into a vector.
 */
final class ProcessingEmbeddingParser {

    private ProcessingEmbeddingParser() {
    }

    static float[] parse(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty() || "[]".equals(trimmed)) {
            return null;
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        String[] parts = trimmed.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
