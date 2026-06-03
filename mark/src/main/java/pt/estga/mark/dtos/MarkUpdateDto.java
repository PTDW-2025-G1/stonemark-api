package pt.estga.mark.dtos;

public record MarkUpdateDto(
        String title,
        String description,
        Boolean active
) { }
