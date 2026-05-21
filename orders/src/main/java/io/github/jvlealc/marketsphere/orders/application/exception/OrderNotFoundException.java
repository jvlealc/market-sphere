package io.github.jvlealc.marketsphere.orders.application.exception;

public final class OrderNotFoundException extends ApplicationException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(Long orderId) {
        super("Not found order with ID: " + orderId);
    }

    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
