package pt.estga.review.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.services.mark.MarkCommandService;
import pt.estga.mark.services.mark.MarkQueryService;
import pt.estga.monument.Monument;
import pt.estga.monument.services.MonumentCommandService;
import pt.estga.monument.services.MonumentQueryService;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;

/**
 * Handles discovery/new-mark review type resolution.
 */
@Component
@RequiredArgsConstructor
public class DiscoveryReviewProcessor implements ReviewProcessor {

    private final MarkCommandService markCommandService;
    private final MarkQueryService markQueryService;
    private final MonumentCommandService monumentCommandService;
    private final MonumentQueryService monumentQueryService;

    @Override
    public ReviewType getSupportedType() { return ReviewType.DISCOVERY; }

    @Override
    @Transactional
    public ResolutionResult resolve(Long submissionId, DiscoveryContext context) {
        Mark mark = null;
        if (context.existingMarkId() != null) {
            mark = markQueryService.findById(context.existingMarkId()).orElseThrow();
        } else if (context.markTitle() != null) {
            mark = markCommandService.create(Mark.builder().title(context.markTitle()).build());
        }

        Monument monument = null;
        if (context.existingMonumentId() != null) {
            monument = monumentQueryService.findById(context.existingMonumentId()).orElseThrow();
        } else if (context.monumentName() != null || context.location() != null) {
            monument = monumentCommandService.create(Monument.builder()
                    .name(context.monumentName())
                    .location(context.location())
                    .build());
        }

        return new ResolutionResult(mark, monument);
    }
}
