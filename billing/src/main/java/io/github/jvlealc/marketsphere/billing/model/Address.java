package io.github.jvlealc.marketsphere.billing.model;

public record Address(
        String postalCode,
        String street,
        String houseNumber,
        String complement,
        String neighborhood,
        String city,
        String state,
        String country
) { }
