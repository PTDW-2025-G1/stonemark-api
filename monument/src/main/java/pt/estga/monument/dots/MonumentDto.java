package pt.estga.monument.dots;

import org.locationtech.jts.geom.Geometry;
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
        AdministrativeDivisionDto division,
        Instant createdAt,
        Instant lastModifiedAt,
        Boolean active
) { }
