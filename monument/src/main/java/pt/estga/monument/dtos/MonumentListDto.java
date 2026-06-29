package pt.estga.monument.dtos;

import pt.estga.commoncore.models.DivisionRef;

public record MonumentListDto(
        Long id,
        String name,
        DivisionRef division,
        Boolean active
) { }