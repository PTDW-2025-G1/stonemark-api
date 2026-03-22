package pt.estga.mark.dtos;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record MarkOccurrenceRequestDto(
    @NotNull Long markId,
    @NotNull Long monumentId,
    Long coverId,
    Instant publishedAt,
    Boolean active
) { }
