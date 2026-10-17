package pt.estga.review.dtos;

import pt.estga.review.enums.MarkCreationSuggestionStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MarkCreationSuggestionDto(
        UUID id,
        MarkCreationSuggestionStatus status,
        int size,
        double coherence,
        Set<Long> memberSubmissionIds,
        Long createdMarkId,
        Instant createdAt,
        Instant updatedAt
) {
}
