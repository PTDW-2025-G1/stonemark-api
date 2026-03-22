package pt.estga.content.dtos;

import java.time.Instant;

public record MarkOccurrenceDto(
    Long id,
    Long markId,
    Long monumentId,
    MarkDto mark,
    Long coverId,
    Long authorId,
    String authorName,
    Instant publishedAt,
    Boolean active
) { }
