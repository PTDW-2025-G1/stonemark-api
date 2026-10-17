package pt.estga.review.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a mark from a mark-creation suggestion.
 */
public record CreateMarkFromSuggestionRequest(
        @NotBlank(message = "A mark title is required")
        @Size(min = 2, max = 100, message = "Mark title must be between 2 and 100 characters")
        String title,
        String description,
        String comment
) {
}
