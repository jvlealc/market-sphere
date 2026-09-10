package io.github.jvlealc.marketsphere.orders.application.customer;

public record CustomerAddress(
        String postalCode,
        String street,
        String houseNumber,
        String complement,
        String neighborhood,
        String city,
        String state,
        String country
) {
}
