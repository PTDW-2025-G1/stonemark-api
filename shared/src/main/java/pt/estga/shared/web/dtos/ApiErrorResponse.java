package pt.estga.shared.web.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Uniform error body returned by every exception handler:
 * {@code { status, code, message, details }}. {@code details} holds per-field
 * validation messages and is omitted when empty.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        int status,
        String code,
        String message,
        Map<String, String> details
) {

    public static ApiErrorResponse of(int status, String code, String message) {
        return new ApiErrorResponse(status, code, message, Map.of());
    }

    public static ApiErrorResponse of(int status, String code, String message, Map<String, String> details) {
        return new ApiErrorResponse(status, code, message, details == null ? Map.of() : details);
    }
}
