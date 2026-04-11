package pt.estga.processing.models;

import pt.estga.mark.entities.Mark;
import java.util.Objects;

/**
 * Aggregated score for a mark.
 *
 * <p>Contains the mark id, optional Mark entity, and a normalized score
 * (named {@code confidence}).
 *
 * <p><b>Confidence:</b> a normalized, weighted/decayed score computed as
 * {@code totalScore / weightSum}. It is a relative ranking metric, not a
 * probability.
 *
 * <p>When a full {@code Mark} is present, {@code markId} must match
 * {@code mark.getId()} or validation fails.
 */
public record MarkScore(Long markId, Mark mark, double confidence) {

	public MarkScore {
		if (mark != null && mark.getId() != null && markId != null && !Objects.equals(markId, mark.getId())) {
			throw new IllegalArgumentException("Inconsistent MarkScore: markId does not match mark.getId() (markId=" + markId + " mark.getId()=" + mark.getId() + ")");
		}
		if (Double.isNaN(confidence) || Double.isInfinite(confidence)) {
			throw new IllegalArgumentException("Invalid confidence value: " + confidence);
		}
	}
}
