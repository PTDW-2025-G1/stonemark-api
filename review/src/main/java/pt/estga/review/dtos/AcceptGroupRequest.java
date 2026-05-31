package pt.estga.review.dtos;

import jakarta.validation.constraints.Size;

public record AcceptGroupRequest(
        @Size(min = 2, max = 100, message = "Mark title must be between 2 and 100 characters")
        String markTitle,

        Long monumentId,

        String monumentName,

        @Size(max = 500)
        String comment) {
}
