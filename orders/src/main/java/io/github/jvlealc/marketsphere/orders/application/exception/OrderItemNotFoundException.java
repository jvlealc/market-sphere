package io.github.jvlealc.marketsphere.orders.application.exception;

public final class OrderItemNotFoundException extends ApplicationException {

    public OrderItemNotFoundException(String message) {
        super(message);
    }

    public OrderItemNotFoundException(Long orderId) {
        super("Not found order item with ID: " + orderId);
    }

    public OrderItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
