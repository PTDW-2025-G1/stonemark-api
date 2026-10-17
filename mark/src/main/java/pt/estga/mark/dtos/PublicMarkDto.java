package pt.estga.mark.dtos;

import java.util.List;
import java.util.UUID;

/** Published mark with its published occurrences and the media backing them. */
public record PublicMarkDto(
        Long id,
        String title,
        String description,
        UUID coverMediaId,
        List<PublicOccurrenceDto> occurrences
) { }
