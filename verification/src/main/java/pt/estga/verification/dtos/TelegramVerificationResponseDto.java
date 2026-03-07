package pt.estga.verification.dtos;

import lombok.Builder;

@Builder
public record TelegramVerificationResponseDto(
        boolean success,
        String message
) {
    public static TelegramVerificationResponseDto success(String message) {
        return TelegramVerificationResponseDto.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static TelegramVerificationResponseDto error(String message) {
        return TelegramVerificationResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }
}
