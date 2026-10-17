package pt.estga.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        UUID userId
) {}
