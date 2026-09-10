package io.github.jvlealc.marketsphere.orders.application.exception;

public final class CustomerNotFoundException extends ExternalServiceException {

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
