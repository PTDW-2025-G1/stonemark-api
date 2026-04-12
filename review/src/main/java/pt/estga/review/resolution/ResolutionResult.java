package pt.estga.review.resolution;

import pt.estga.mark.entities.Mark;
import pt.estga.monument.Monument;

/**
 * Result of a resolution attempt: the resolved mark and monument (either existing
 * or newly created provisional/phantom entities).
 */
public record ResolutionResult(Mark mark, Monument monument) {
}
