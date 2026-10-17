package pt.estga.mark.dtos;

import pt.estga.shared.core.enums.PublicationStatus;

import java.util.UUID;

public record MarkDto(
        UUID id,
        String title,
        String description,
        UUID coverMediaId,
        PublicationStatus status
) { }
