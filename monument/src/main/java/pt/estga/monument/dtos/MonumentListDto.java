package pt.estga.monument.dtos;

import java.util.UUID;

public record MonumentListDto(
        UUID id,
        String name,
        Boolean active
) { }
