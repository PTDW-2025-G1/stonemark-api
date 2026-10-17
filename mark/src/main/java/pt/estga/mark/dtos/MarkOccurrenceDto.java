package pt.estga.mark.dtos;

import java.util.UUID;

import pt.estga.shared.core.enums.PublicationStatus;

import java.time.Instant;

public record MarkOccurrenceDto(
        UUID id,
        UUID markId,
        UUID monumentId,
        Double latitude,
        Double longitude,
        String period,
        PublicationStatus status,
        Instant publishedAt,
        MarkDto mark
) { }
