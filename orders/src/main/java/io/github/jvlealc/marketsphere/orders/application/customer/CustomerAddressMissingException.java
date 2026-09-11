package io.github.jvlealc.marketsphere.orders.application.customer;

import io.github.jvlealc.marketsphere.orders.application.ApplicationException;

public final class CustomerAddressMissingException extends ApplicationException {

    public CustomerAddressMissingException(String message) {
        super(message);
    }
}
