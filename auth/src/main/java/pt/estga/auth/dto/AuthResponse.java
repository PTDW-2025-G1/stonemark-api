package pt.estga.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        Long userId
) {}
