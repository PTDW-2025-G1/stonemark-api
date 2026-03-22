package pt.estga.mark.dtos;

public record MarkRequestDto(
        String description,
        Long coverId,
        Boolean active
) { }
