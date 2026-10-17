package pt.estga.monument.dtos;

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
        Instant createdAt,
        Instant lastModifiedAt,
        Boolean active
) { }
