package io.github.jvlealc.marketsphere.orders.exception.client.products;

import io.github.jvlealc.marketsphere.orders.exception.client.ClientValidationException;

public final class ProductClientNotFoundException extends ClientValidationException {
    public ProductClientNotFoundException(String field, String message) {
        super(field, message);
    }
}
