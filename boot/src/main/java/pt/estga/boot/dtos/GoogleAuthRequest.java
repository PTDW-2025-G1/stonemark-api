package pt.estga.boot.dtos;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank String token
) {}
