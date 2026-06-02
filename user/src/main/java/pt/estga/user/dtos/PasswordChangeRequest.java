package pt.estga.user.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
