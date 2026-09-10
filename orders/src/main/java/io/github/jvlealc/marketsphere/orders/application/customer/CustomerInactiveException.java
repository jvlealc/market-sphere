package io.github.jvlealc.marketsphere.orders.application.customer;

import io.github.jvlealc.marketsphere.orders.application.ExternalServiceException;

public final class CustomerInactiveException extends ExternalServiceException {

    public CustomerInactiveException(String message) {
        super(message);
    }
}
