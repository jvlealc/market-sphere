package io.github.jvlealc.marketsphere.billing.model;

public record Address(
        String postalCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String country
) { }
