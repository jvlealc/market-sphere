package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.rest.order;

public record OrderCustomerResponse(
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
        String country
) {
}
