package io.github.jvlealc.marketsphere.customers.exception;

public class CustomerNationalIdAlreadyInUseException extends RuntimeException {
    public CustomerNationalIdAlreadyInUseException() {
        super("National ID already in use.");
    }

    public CustomerNationalIdAlreadyInUseException(String customerNationalId) {
        super("National ID already in use: " + customerNationalId);
    }

    public CustomerNationalIdAlreadyInUseException(String message, Throwable cause) {
        super(message, cause);
    }
}
