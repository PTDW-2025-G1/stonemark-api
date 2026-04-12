package pt.estga.review.dtos;

import pt.estga.review.enums.ReviewDecision;

import java.time.Instant;

/**
 * Response DTO for a persisted review.
 */
public record ReviewResponseDto(
        Long id,
        Long submissionId,
        ReviewDecision decision,
        Long selectedMarkId,
        String selectedMarkTitle,
        Long reviewerId,
        String reviewerName,
        Instant reviewedAt,
        String comment) {
}
