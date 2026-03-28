package pt.estga.territory.exceptions;

/**
 * Thrown when an operation receives a country code that is not supported or not present in the database.
 */
public class UnsupportedCountryException extends RuntimeException {

    public UnsupportedCountryException() {
        super();
    }

    public UnsupportedCountryException(String message) {
        super(message);
    }

    public UnsupportedCountryException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedCountryException(Throwable cause) {
        super(cause);
    }
}
