package pt.estga.processing.models;

public record MarkScore(Long markId, double confidence) {

    public MarkScore {
        if (Double.isNaN(confidence) || Double.isInfinite(confidence)) {
            throw new IllegalArgumentException("Invalid confidence value: " + confidence);
        }
    }
}
