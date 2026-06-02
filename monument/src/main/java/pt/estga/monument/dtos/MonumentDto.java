package pt.estga.monument.dtos;

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
        String address,
        String postalCode,
        AdministrativeDivisionDto division,
        Instant createdAt,
        Instant lastModifiedAt,
        Boolean active
) { }
