package pt.estga.mark.dtos;

import java.util.UUID;

import pt.estga.shared.core.enums.PublicationStatus;

public record MarkOccurrenceUpdateDto(
        PublicationStatus status,
        String period,
        UUID monumentId,
        Double latitude,
        Double longitude
) { }
