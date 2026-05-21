package io.github.jvlealc.marketsphere.orders.application.exception;

public final class InvalidOutboxMessageException extends ApplicationException {

    public InvalidOutboxMessageException(String message) {
        super(message);
    }

    public InvalidOutboxMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
