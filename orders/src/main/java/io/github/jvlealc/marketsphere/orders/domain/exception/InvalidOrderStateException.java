package io.github.jvlealc.marketsphere.orders.domain.exception;

public class InvalidOrderStateException extends OrderDomainException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
