package io.github.jvlealc.marketsphere.orders.exception.client.customers;

import io.github.jvlealc.marketsphere.orders.exception.client.ClientValidationException;

public final class CustomerClientNotFoundException extends ClientValidationException {
    public CustomerClientNotFoundException(String field, String message) {
        super(field, message);
    }
}
