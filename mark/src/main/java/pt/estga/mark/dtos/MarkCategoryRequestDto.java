package pt.estga.mark.dtos;

public record MarkCategoryRequestDto(
        String name,
        String description,
        Long parentId
) { }
