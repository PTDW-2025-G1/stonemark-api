package pt.estga.mark.dtos;

import pt.estga.shared.core.enums.PublicationStatus;

public record MarkOccurrenceUpdateDto(
        PublicationStatus status,
        String period,
        Long monumentId,
        Double latitude,
        Double longitude
) { }
