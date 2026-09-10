package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.client.feign.customer;

public record CustomerRepresentation(
        Long id,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        AddressRepresentation address,
        boolean active
) {
}
