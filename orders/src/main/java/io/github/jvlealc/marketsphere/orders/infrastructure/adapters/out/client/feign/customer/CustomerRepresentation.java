package io.github.jvlealc.marketsphere.orders.infrastructure.client.customer;

public record CustomerRepresentation(
        Long id,
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
) { }
