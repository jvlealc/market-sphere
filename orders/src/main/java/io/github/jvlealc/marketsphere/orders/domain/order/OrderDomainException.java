package io.github.jvlealc.marketsphere.orders.domain.order;

public abstract class OrderDomainException extends RuntimeException {

    protected OrderDomainException(String message) {
        super(message);
    }

    protected OrderDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
