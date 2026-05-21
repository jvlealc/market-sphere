package io.github.jvlealc.marketsphere.orders.infrastructure.exception;

public class OutboxPersistenceException extends InfraException {

    public OutboxPersistenceException(String message) {
        super(message);
    }

    public OutboxPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
