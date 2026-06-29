package pt.estga.monument.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MonumentRequestDto(
        @NotBlank String name,
        String description,
        String protectionTitle,
        String website,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String address,
        String postalCode,
        String divisionCode,
        Boolean active
) { }
