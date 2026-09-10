package io.github.jvlealc.marketsphere.orders.application.outbox;

import io.github.jvlealc.marketsphere.orders.application.ApplicationException;

public final class InvalidOutboxMessageException extends ApplicationException {

    public InvalidOutboxMessageException(String message) {
        super(message);
    }

    public InvalidOutboxMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
