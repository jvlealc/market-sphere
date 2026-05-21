package io.github.jvlealc.marketsphere.orders.application.exception;

public final class CustomerNotFoundException extends ExternalServiceException {

    public CustomerNotFoundException(String field, String message) {
        super(field, message);
    }
}
