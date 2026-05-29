package pt.estga.review.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentRepository;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;

/**
 * Processor for MATCH review type: resolves existing entities only.
 */
@Component
@RequiredArgsConstructor
public class MatchReviewProcessor implements ReviewProcessor {

    private final MarkRepository markRepository;
    private final MonumentRepository monumentRepository;

    @Override
    public ReviewType getSupportedType() { return ReviewType.MATCH; }

    @Override
    public ResolutionResult resolve(Long submissionId, DiscoveryContext context) {
        Mark mark = null;
        Monument monument = null;

        if (context.existingMarkId() != null) {
            mark = markRepository.findById(context.existingMarkId()).orElseThrow();
        }
        if (context.existingMonumentId() != null) {
            monument = monumentRepository.findById(context.existingMonumentId()).orElseThrow();
        }

        return new ResolutionResult(mark, monument);
    }
}
