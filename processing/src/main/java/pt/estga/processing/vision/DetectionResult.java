package pt.estga.processing.vision;

public record DetectionResult(
        boolean isMasonMark,
        float[] embedding
) { }
