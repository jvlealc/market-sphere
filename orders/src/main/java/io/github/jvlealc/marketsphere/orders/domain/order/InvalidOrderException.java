package io.github.jvlealc.marketsphere.orders.domain.order;

public class InvalidOrderException extends OrderDomainException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
