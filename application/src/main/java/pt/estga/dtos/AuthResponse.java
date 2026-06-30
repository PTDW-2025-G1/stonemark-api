package pt.estga.dtos;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        Long userId
) {}
