package io.github.jvlealc.marketsphere.orders.client.products;

import io.github.jvlealc.marketsphere.orders.client.ClientException;

public final class ProductClientNotFoundException extends ClientException {
    public ProductClientNotFoundException(String field, String message) {
        super(field, message);
    }
}
