package pt.estga.boot.dtos;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        Long userId
) {}
