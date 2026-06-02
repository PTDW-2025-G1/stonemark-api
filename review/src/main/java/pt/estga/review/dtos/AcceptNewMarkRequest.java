package pt.estga.review.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a new mark and accept the submission.
 */
public record AcceptNewMarkRequest(
        @NotBlank(message = "Mark title is required when creating a new mark")
        @Size(min = 2, max = 100, message = "Mark title must be between 2 and 100 characters")
        String markTitle,
        String comment) {
}
