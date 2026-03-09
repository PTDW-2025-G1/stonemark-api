package pt.estga.verification.dtos;

import lombok.Builder;

@Builder
public record ChatbotVerificationResponseDto(
        boolean success,
        String message
) {
    public static ChatbotVerificationResponseDto success(String message) {
        return ChatbotVerificationResponseDto.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static ChatbotVerificationResponseDto error(String message) {
        return ChatbotVerificationResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }
}
