package pt.estga.sharedweb.dtos;

/**
 * DTO for conveying an operation outcome and a human-readable message.
 */
public record MessageResponseDto(boolean success, String message) {

	public static MessageResponseDto success(String message) {
		return new MessageResponseDto(true, message);
	}

	public static MessageResponseDto error(String message) {
		return new MessageResponseDto(false, message);
	}
}
