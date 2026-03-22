package pt.estga.content.dtos;

public record MarkRequestDto(
        String description,
        Long coverId,
        Boolean active
) { }
