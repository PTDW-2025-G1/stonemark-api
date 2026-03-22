package pt.estga.monument.dots;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MonumentRequestDto(
        @NotBlank String name,
        String description,
        String protectionTitle,
        String website,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String street,
        String houseNumber,
        Long parishId,
        Long municipalityId,
        Long districtId,
        Boolean active
) { }
