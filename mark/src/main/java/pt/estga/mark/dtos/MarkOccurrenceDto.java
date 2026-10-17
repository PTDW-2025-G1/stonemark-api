package pt.estga.mark.dtos;

import pt.estga.shared.core.enums.PublicationStatus;

import java.time.Instant;

public record MarkOccurrenceDto(
        Long id,
        Long markId,
        Long monumentId,
        Double latitude,
        Double longitude,
        String period,
        PublicationStatus status,
        Instant publishedAt,
        MarkDto mark
) { }
