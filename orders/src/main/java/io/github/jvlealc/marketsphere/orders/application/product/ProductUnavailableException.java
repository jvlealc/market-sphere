package io.github.jvlealc.marketsphere.orders.application.product;

import io.github.jvlealc.marketsphere.orders.application.ApplicationException;

public final class ProductUnavailableException extends ApplicationException {

    public ProductUnavailableException(String message) {
        super(message);
    }
}
