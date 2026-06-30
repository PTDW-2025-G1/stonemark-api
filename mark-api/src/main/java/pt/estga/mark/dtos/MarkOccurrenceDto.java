package pt.estga.mark.dtos;

import java.time.Instant;

public record MarkOccurrenceDto(
    Long id,
    Long markId,
    Long monumentId,
    MarkDto mark,
    Instant publishedAt,
    Boolean active
) { }
