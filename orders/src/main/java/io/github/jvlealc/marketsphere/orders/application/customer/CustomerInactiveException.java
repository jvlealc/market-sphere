package io.github.jvlealc.marketsphere.orders.application.customer;

import io.github.jvlealc.marketsphere.orders.application.ApplicationException;

public final class CustomerInactiveException extends ApplicationException {

    public CustomerInactiveException(String message) {
        super(message);
    }
}
