package pt.estga.monument.dtos;

import pt.estga.territory.dtos.AdministrativeDivisionDto;

public record MonumentListDto(
        Long id,
        String name,
        AdministrativeDivisionDto division,
        Boolean active
) { }