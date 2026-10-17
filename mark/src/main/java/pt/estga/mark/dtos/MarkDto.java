package pt.estga.mark.dtos;

import java.util.UUID;

public record MarkDto(
        Long id,
        String title,
        String description,
        float[] embedding,
        UUID coverId,
        Boolean active
) { }
