package pt.estga.sharedweb.exceptions;

public class InvalidTelegramTokenException extends RuntimeException {
    public InvalidTelegramTokenException(String message) {
        super(message);
    }

    public InvalidTelegramTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
