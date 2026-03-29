package pt.estga.vision;

public record DetectionResult(
        boolean isMasonMark,
        float[] embedding
) { }
