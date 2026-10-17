package pt.estga.mark.dtos;

import java.util.UUID;

/** Catalogue entry for a published mark. */
public record PublicMarkSummaryDto(
        UUID id,
        String title,
        String description,
        UUID coverMediaId
) { }
