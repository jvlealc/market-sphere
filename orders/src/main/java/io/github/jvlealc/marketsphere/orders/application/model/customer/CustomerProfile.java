package io.github.jvlealc.marketsphere.orders.application.model.customer;

public record CustomerProfile(
        Long customerId,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        String postalCode,
        String street,
        String houseNumber,
        String complement,
        String neighborhood,
        String city,
        String state,
        String country,
        boolean active
) {
}