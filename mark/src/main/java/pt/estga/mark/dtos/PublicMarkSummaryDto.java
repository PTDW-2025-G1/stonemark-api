package pt.estga.mark.dtos;

import java.util.UUID;

/** Catalogue entry for a published mark. */
public record PublicMarkSummaryDto(
        Long id,
        String title,
        String description,
        UUID coverMediaId
) { }
