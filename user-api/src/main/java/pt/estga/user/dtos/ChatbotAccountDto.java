package pt.estga.user.dtos;

public record ChatbotAccountDto(
        Long id,
        Long userId,
        String platform,
        String value
) { }
