package pt.estga.file.exceptions;

/**
 * Runtime exception indicating a failure while persisting media metadata.
 */
public class MediaPersistenceException extends RuntimeException {

    public MediaPersistenceException() {
        super();
    }

    public MediaPersistenceException(String message) {
        super(message);
    }

    public MediaPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaPersistenceException(Throwable cause) {
        super(cause);
    }
}
