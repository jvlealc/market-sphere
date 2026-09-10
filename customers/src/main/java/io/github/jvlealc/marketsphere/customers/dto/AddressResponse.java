package io.github.jvlealc.marketsphere.customers.dto;

public record AddressResponse(
        Long id,
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