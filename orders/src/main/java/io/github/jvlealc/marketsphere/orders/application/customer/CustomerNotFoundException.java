package io.github.jvlealc.marketsphere.orders.application.customer;

import io.github.jvlealc.marketsphere.orders.application.ApplicationException;

public final class CustomerNotFoundException extends ApplicationException {

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
