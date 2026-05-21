package io.github.jvlealc.marketsphere.orders.application.exception;

public final class CustomerInactiveException extends ExternalServiceException {

    public CustomerInactiveException(String field, String message) {
        super(field, message);
    }
}
