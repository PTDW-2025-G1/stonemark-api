package pt.estga.mark.dtos;

import java.util.UUID;

public record MarkUpdateDto(
        String title,
        String description,
        UUID goldenExampleId,
        Boolean active
) { }
