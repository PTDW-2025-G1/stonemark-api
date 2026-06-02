package pt.estga.review.mappers;

import pt.estga.review.dtos.ReviewResponseDto;
import pt.estga.review.entities.MarkEvidenceReview;

public class ReviewMapper {

    private ReviewMapper() {}

    public static ReviewResponseDto toDto(MarkEvidenceReview review) {
        if (review == null) return null;
        return new ReviewResponseDto(
                review.getId(),
                review.getSubmissionId(),
                review.getDecision(),
                review.getSelectedMarkId(),
                null,
                review.getReviewedById(),
                null,
                review.getReviewedAt(),
                review.getComment()
        );
    }
}