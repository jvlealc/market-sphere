package io.github.jvlealc.marketsphere.orders.client.customers;

public record CustomerRepresentation(
        Long id,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        String postalCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String country,
        boolean active
) { }
