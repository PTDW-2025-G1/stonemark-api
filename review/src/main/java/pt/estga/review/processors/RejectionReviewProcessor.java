package pt.estga.review.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;

/**
 * Processor for REJECTION review type: returns empty resolution.
 */
@Component
@RequiredArgsConstructor
public class RejectionReviewProcessor implements ReviewProcessor {

    @Override
    public ReviewType getSupportedType() { return ReviewType.REJECTION; }

    @Override
    public ResolutionResult resolve(Long submissionId, DiscoveryContext context) {
        return new ResolutionResult(null, null);
    }
}
