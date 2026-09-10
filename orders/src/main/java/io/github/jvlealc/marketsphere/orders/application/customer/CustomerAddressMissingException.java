package io.github.jvlealc.marketsphere.orders.application.customer;

import io.github.jvlealc.marketsphere.orders.application.ExternalServiceException;

public final class CustomerAddressMissingException extends ExternalServiceException {

    public CustomerAddressMissingException(String message) {
        super(message);
    }
}
