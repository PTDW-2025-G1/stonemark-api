package pt.estga.monument.dtos;

import pt.estga.shared.core.models.DivisionRef;

public record MonumentListDto(
        Long id,
        String name,
        DivisionRef division,
        Boolean active
) { }