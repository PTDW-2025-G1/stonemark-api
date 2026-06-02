package pt.estga.monument.dtos;

import jakarta.validation.constraints.NotBlank;

public record PolygonSearchRequest(
        @NotBlank String geoJson
) { }
