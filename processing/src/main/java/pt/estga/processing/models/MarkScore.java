package pt.estga.processing.models;

import pt.estga.mark.entities.Mark;

/**
 * Final per-mark score produced by aggregation: mark id, mark entity and confidence.
 */
public record MarkScore(Long markId, Mark mark, double confidence) {}
