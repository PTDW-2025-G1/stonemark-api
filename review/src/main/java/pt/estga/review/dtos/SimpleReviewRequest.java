package pt.estga.review.dtos;

/**
 * Simple request carrying an optional comment for review actions.
 */
public record SimpleReviewRequest(String comment) {
}
