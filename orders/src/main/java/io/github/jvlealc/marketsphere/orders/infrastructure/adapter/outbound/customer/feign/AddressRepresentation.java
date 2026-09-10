package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.customer.feign;

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
