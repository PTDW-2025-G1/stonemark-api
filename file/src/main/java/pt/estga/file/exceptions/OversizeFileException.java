package pt.estga.file.exceptions;

/**
 * Runtime exception indicating an uploaded file exceeded configured size limits.
 */
public class OversizeFileException extends RuntimeException {

    public OversizeFileException() {
        super();
    }

    public OversizeFileException(String message) {
        super(message);
    }

    public OversizeFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public OversizeFileException(Throwable cause) {
        super(cause);
    }
}

