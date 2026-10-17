package pt.estga.review.dtos;

import java.util.UUID;

import pt.estga.review.enums.ReviewDecision;

import java.time.Instant;

/**
 * Response DTO for a persisted review.
 */
public record ReviewResponseDto(
        UUID id,
        UUID submissionId,
        ReviewDecision decision,
        UUID selectedMarkId,
        String selectedMarkTitle,
        UUID reviewerId,
        String reviewerName,
        Instant reviewedAt,
        String comment) {
}
