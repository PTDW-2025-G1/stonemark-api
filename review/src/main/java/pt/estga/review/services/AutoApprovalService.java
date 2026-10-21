package pt.estga.review.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.estga.processing.api.ProcessingModerationView;
import pt.estga.processing.api.ProcessingReviewApi;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.enums.ProcessingStatus;
import pt.estga.review.repositories.MarkEvidenceReviewRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Approves occurrences the system is confident and unambiguous about, without a
 * human. It only links a submission to an existing mark whose match is clear — it
 * never creates a mark. Runs only when {@code review.auto-approve.enabled=true},
 * which requires the processing pipeline to be producing suggestions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoApprovalService {

    private final ProcessingReviewApi processingApi;
    private final MarkEvidenceReviewRepository reviewRepository;
    private final ReviewService reviewService;

    @Value("${review.auto-approve.min-confidence:0.9}")
    private double minConfidence;

    @Value("${review.auto-approve.min-margin:0.1}")
    private double minMargin;

    /**
     * Approves every eligible candidate. Returns how many were approved.
     *
     * <p>Deliberately not transactional as a whole: each approval commits on its own
     * (via {@link ReviewService#autoApprove}), so one failure cannot roll back the
     * batch and a single bad candidate never blocks the rest.
     */
    public int approveEligible() {
        List<ProcessingModerationView> candidates = processingApi.findProcessingByConfidence(minConfidence, 1.0);
        int approved = 0;
        for (ProcessingModerationView candidate : candidates) {
            if (candidate.status() != ProcessingStatus.COMPLETED) {
                continue;
            }
            if (reviewRepository.existsBySubmissionId(candidate.submissionId())) {
                continue;
            }
            UUID markId = confidentMatch(processingApi.findSuggestions(candidate.processingId()));
            if (markId == null) {
                continue;
            }
            try {
                reviewService.autoApprove(candidate.submissionId(), markId);
                approved++;
            } catch (Exception e) {
                log.warn("Auto-approval failed for submission {}: {}",
                        candidate.submissionId(), e.getMessage());
            }
        }
        return approved;
    }

    /**
     * The mark to accept, or {@code null} when the best suggestion is not confident
     * enough or not clearly ahead of the runner-up.
     */
    private UUID confidentMatch(List<MarkSuggestionDto> suggestions) {
        List<MarkSuggestionDto> ranked = suggestions.stream()
                .sorted(Comparator.comparingDouble(MarkSuggestionDto::getConfidence).reversed())
                .toList();
        if (ranked.isEmpty()) {
            return null;
        }
        MarkSuggestionDto best = ranked.getFirst();
        if (best.getConfidence() < minConfidence) {
            return null;
        }
        if (ranked.size() > 1 && best.getConfidence() - ranked.get(1).getConfidence() < minMargin) {
            return null;
        }
        return best.getMarkId();
    }
}
