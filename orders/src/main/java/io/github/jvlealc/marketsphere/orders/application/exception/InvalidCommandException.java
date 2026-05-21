package io.github.jvlealc.marketsphere.orders.application.exception;

public final class InvalidCommandException extends ApplicationException {

    public InvalidCommandException(String message) {
        super(message);
    }

    public InvalidCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
