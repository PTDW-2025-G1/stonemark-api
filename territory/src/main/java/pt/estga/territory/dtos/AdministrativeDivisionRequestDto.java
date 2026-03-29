package pt.estga.territory.dtos;

import lombok.Builder;

@Builder
public record AdministrativeDivisionRequestDto(
        Integer osmAdminLevel,
        String name,
        Long parentId,
        Boolean active
) { }
