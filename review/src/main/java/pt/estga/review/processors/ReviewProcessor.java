package pt.estga.review.processors;

import pt.estga.review.enums.ReviewType;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.models.ResolutionResult;

/**
 * Strategy for resolving entities required by a review.
 */
public interface ReviewProcessor {
    ReviewType getSupportedType();

    ResolutionResult resolve(Long submissionId, DiscoveryContext context);
}
