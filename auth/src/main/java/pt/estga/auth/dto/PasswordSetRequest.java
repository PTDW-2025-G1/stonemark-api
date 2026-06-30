package pt.estga.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordSetRequest(
        @NotBlank @Size(min = 8, max = 128) String password
) {}
