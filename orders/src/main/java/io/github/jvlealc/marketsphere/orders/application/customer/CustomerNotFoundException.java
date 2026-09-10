package io.github.jvlealc.marketsphere.orders.application.customer;

import io.github.jvlealc.marketsphere.orders.application.ExternalServiceException;

public final class CustomerNotFoundException extends ExternalServiceException {

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
