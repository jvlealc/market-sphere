package io.github.jvlealc.marketsphere.orders.application.customer;

public record CustomerProfile(
        Long customerId,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        CustomerAddress address,
        boolean active
) {

    public boolean hasAddress() {
        return address != null;
    }
}
