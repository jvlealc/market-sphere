package io.github.jvlealc.marketsphere.orders.application.exception;

public final class OrderItemsMissingException extends ApplicationException {

    public OrderItemsMissingException(Long orderId) {
        super("Order items are missing for order ID: " + orderId);
    }

    public OrderItemsMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}
