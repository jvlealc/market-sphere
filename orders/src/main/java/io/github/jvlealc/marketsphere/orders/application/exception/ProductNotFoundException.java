package io.github.jvlealc.marketsphere.orders.application.exception;

public final class ProductNotFoundException extends ExternalServiceException {

    public ProductNotFoundException(String field, String message) {
        super(field, message);
    }
}
