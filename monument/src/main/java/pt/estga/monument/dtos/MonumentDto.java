package pt.estga.monument.dtos;

import pt.estga.commoncore.models.DivisionRef;

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
        DivisionRef division,
        Instant createdAt,
        Instant lastModifiedAt,
        Boolean active
) { }
