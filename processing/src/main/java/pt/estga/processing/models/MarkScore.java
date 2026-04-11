package pt.estga.processing.models;

import pt.estga.mark.entities.Mark;
import java.util.Objects;

/**
 * Final per-mark score produced by aggregation: mark id, mark entity and confidence.
 * <p>
 * We validate consistency between the provided markId and the mark.getId() when
 * a full Mark entity is present to fail-fast on mapping inconsistencies.
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
