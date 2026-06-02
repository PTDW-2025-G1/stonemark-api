package pt.estga.review.processors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.repositories.MarkRepository;
import pt.estga.review.dtos.DiscoveryContext;
import pt.estga.review.enums.ReviewType;
import pt.estga.review.models.ResolutionResult;

@Component
@RequiredArgsConstructor
public class DiscoveryReviewProcessor implements ReviewProcessor {

    private final MarkRepository markRepository;

    @Override
    public ReviewType getSupportedType() { return ReviewType.DISCOVERY; }

    @Override
    @Transactional
    public ResolutionResult resolve(Long submissionId, DiscoveryContext context) {
        Mark mark = null;
        if (context.existingMarkId() != null) {
            mark = markRepository.findById(context.existingMarkId()).orElseThrow();
        } else if (context.markTitle() != null) {
            mark = markRepository.save(Mark.builder().title(context.markTitle()).build());
        }

        return new ResolutionResult(mark);
    }
}
