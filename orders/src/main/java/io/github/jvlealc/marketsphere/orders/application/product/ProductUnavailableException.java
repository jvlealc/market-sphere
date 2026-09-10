package io.github.jvlealc.marketsphere.orders.application.product;

import io.github.jvlealc.marketsphere.orders.application.ExternalServiceException;

public final class ProductUnavailableException extends ExternalServiceException {

    public ProductUnavailableException(String message) {
        super(message);
    }
}
