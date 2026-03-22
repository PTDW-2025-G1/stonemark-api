package pt.estga.detection;

public record DetectionResult(
        boolean isMasonMark,
        float[] embedding
) { }
