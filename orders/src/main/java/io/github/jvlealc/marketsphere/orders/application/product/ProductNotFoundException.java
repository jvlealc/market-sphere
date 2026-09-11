package io.github.jvlealc.marketsphere.orders.application.product;

import io.github.jvlealc.marketsphere.orders.application.ApplicationException;

public final class ProductNotFoundException extends ApplicationException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
