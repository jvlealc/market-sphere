package io.github.jvlealc.marketsphere.orders.domain.order;

public class InvalidOrderStateException extends OrderDomainException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
