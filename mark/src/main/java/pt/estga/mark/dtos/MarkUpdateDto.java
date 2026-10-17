package pt.estga.mark.dtos;

import pt.estga.shared.core.enums.PublicationStatus;

import java.util.UUID;

public record MarkUpdateDto(
        String title,
        String description,
        UUID coverMediaId,
        PublicationStatus status
) { }
