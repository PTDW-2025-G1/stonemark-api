package pt.estga.territory.dtos;

import lombok.Builder;

@Builder
public record AdministrativeDivisionDto(
    Long id,
    String name,
    Long parentId,
    Integer monumentsCount
) {}
