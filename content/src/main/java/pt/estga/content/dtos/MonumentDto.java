package pt.estga.content.dtos;

import pt.estga.territory.dtos.AdministrativeDivisionDto;

import java.time.Instant;

public record MonumentDto(
        Long id,
        String name,
        String description,
        String protectionTitle,
        String website,
        Double latitude,
        Double longitude,
        String street,
        String houseNumber,
        AdministrativeDivisionDto parish,
        AdministrativeDivisionDto municipality,
        AdministrativeDivisionDto district,
        Instant createdAt,
        Instant lastModifiedAt,
        Boolean active
) { }
