package io.github.jvlealc.marketsphere.orders.application.exception;

public final class ProductUnavailableException extends ExternalServiceException {

    public ProductUnavailableException(String field, String message) {
        super(field, message);
    }
}
