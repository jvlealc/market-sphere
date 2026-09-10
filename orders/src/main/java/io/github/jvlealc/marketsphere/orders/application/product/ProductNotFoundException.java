package io.github.jvlealc.marketsphere.orders.application.product;

import io.github.jvlealc.marketsphere.orders.application.ExternalServiceException;

public final class ProductNotFoundException extends ExternalServiceException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
