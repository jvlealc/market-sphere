package io.github.jvlealc.marketsphere.orders.domain.order;

public class InvalidPaymentInfoException extends InvalidOrderException {

    public InvalidPaymentInfoException(String message) {
        super(message);
    }
}
