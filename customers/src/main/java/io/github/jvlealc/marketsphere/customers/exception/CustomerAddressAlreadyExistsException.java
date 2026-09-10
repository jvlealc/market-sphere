package io.github.jvlealc.marketsphere.customers.exception;

public class CustomerAddressAlreadyExistsException extends RuntimeException {

    public CustomerAddressAlreadyExistsException(Long customerId) {
        super("An address is already registered for customer ID: " + customerId);
    }
}
