package pt.estga.verification.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotVerificationRequestDto(
        @NotBlank @Size(min = 6, max = 6) String code
) { }
