package pt.estga.review.mappers;

import org.springframework.stereotype.Component;
import pt.estga.review.dtos.ReviewResponseDto;
import pt.estga.review.entities.MarkEvidenceReview;

@Component
public class ReviewMapper {

    public ReviewResponseDto toDto(MarkEvidenceReview review) {
        if (review == null) return null;
        String reviewerName = review.getReviewedBy() != null
                ? review.getReviewedBy().getFirstName() + " " + review.getReviewedBy().getLastName()
                : "System";
        return new ReviewResponseDto(
                review.getId(),
                review.getSubmission() != null ? review.getSubmission().getId() : null,
                review.getDecision(),
                review.getSelectedMark() != null ? review.getSelectedMark().getId() : null,
                review.getSelectedMark() != null ? review.getSelectedMark().getTitle() : null,
                review.getReviewedBy() != null ? review.getReviewedBy().getId() : null,
                reviewerName,
                review.getReviewedAt(),
                review.getComment()
        );
    }
}