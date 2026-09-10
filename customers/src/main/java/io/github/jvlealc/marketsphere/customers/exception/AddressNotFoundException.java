package io.github.jvlealc.marketsphere.customers.exception;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(Long addressId, Long customerId) {
        super("No Address found with ID '%s' and customer ID '%s'".formatted(addressId, customerId));
    }
}
