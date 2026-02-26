package io.github.jvlealc.marketsphere.customers.exception;

public class CustomerEmailAlreadyInUseException extends RuntimeException {
    public CustomerEmailAlreadyInUseException() {
        super("Email already in use.");
    }

    public CustomerEmailAlreadyInUseException(String customerEmail) {
        super("Email already in use: " + customerEmail);
    }

    public CustomerEmailAlreadyInUseException(String message, Throwable cause) {
        super(message, cause);
    }
}
