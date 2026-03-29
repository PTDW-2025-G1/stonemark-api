package pt.estga.monument.dots;

import pt.estga.territory.dtos.AdministrativeDivisionDto;

public record MonumentListDto(
        Long id,
        String name,
        AdministrativeDivisionDto division,
        Boolean active
) { }