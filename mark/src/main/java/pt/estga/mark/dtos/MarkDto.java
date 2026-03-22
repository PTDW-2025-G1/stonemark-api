package pt.estga.mark.dtos;

public record MarkDto(
        Long id,
        String title,
        String description,
        float[] embedding,
        Long coverId,
        Boolean active
) { }
