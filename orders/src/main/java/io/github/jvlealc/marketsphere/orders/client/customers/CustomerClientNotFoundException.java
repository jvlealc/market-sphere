package io.github.jvlealc.marketsphere.orders.client.customers;

import io.github.jvlealc.marketsphere.orders.client.ClientException;

public final class CustomerClientNotFoundException extends ClientException {
    public CustomerClientNotFoundException(String field, String message) {
        super(field, message);
    }
}
