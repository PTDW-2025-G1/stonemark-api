package pt.estga.content.dtos;

import pt.estga.territory.dtos.AdministrativeDivisionDto;

public record MonumentListDto(
        Long id,
        String name,
        AdministrativeDivisionDto parish,
        AdministrativeDivisionDto municipality,
        AdministrativeDivisionDto district,
        Boolean active
) { }