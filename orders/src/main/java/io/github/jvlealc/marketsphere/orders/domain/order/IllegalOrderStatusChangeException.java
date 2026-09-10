package io.github.jvlealc.marketsphere.orders.domain.order;

public class IllegalOrderStatusChangeException extends OrderDomainException {

    public IllegalOrderStatusChangeException(String message) {
        super(message);
    }

    public IllegalOrderStatusChangeException(OrderStatus from, OrderStatus to) {
        super("Only " + from + " orders can be marked as " + to);
    }
}
