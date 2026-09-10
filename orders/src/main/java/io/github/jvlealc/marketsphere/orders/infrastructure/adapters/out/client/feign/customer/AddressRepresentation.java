package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.client.feign.customer;

public record AddressRepresentation(
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
