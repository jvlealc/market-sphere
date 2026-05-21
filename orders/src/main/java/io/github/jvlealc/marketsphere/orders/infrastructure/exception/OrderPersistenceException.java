package io.github.jvlealc.marketsphere.orders.infrastructure.exception;

public class OrderPersistenceException extends InfrastructureException {

    public OrderPersistenceException(String message) {
        super(message);
    }

    public OrderPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
