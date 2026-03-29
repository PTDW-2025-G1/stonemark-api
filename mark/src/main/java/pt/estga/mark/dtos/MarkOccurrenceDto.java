package pt.estga.mark.dtos;

import java.time.Instant;
import java.util.UUID;

public record MarkOccurrenceDto(
    Long id,
    Long markId,
    Long monumentId,
    MarkDto mark,
    UUID coverId,
    Long authorId,
    String authorName,
    Instant publishedAt,
    Boolean active
) { }
