package io.github.jvlealc.marketsphere.orders.domain.exception;

import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;

public class IllegalOrderStatusChangeException extends OrderDomainException {

    public IllegalOrderStatusChangeException(String message) {
        super(message);
    }

    public IllegalOrderStatusChangeException(OrderStatus from, OrderStatus to) {
        super("Only " + from + " orders can be marked as " + to);
    }
}
