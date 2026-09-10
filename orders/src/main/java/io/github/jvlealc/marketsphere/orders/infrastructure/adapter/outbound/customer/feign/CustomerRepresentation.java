package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.customer.feign;

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
