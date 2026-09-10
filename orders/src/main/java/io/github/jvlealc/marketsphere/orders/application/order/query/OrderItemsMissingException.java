package io.github.jvlealc.marketsphere.orders.application.order.query;

import io.github.jvlealc.marketsphere.orders.application.ApplicationException;

public final class OrderItemsMissingException extends ApplicationException {

    public OrderItemsMissingException(Long orderId) {
        super("Order items are missing for order ID: " + orderId);
    }

    public OrderItemsMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}
