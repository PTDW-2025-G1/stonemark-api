package pt.estga.review.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.services.mark.MarkQueryService;
import pt.estga.monument.Monument;
import pt.estga.monument.services.MonumentQueryService;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;

/**
 * Processor for MATCH review type: resolves existing entities only.
 */
@Component
@RequiredArgsConstructor
public class MatchReviewProcessor implements ReviewProcessor {

    private final MarkQueryService markQueryService;
    private final MonumentQueryService monumentQueryService;

    @Override
    public ReviewType getSupportedType() { return ReviewType.MATCH; }

    @Override
    public ResolutionResult resolve(Long submissionId, DiscoveryContext context) {
        Mark mark = null;
        Monument monument = null;

        if (context.existingMarkId() != null) {
            mark = markQueryService.findById(context.existingMarkId()).orElseThrow();
        }
        if (context.existingMonumentId() != null) {
            monument = monumentQueryService.findById(context.existingMonumentId()).orElseThrow();
        }

        return new ResolutionResult(mark, monument);
    }
}
