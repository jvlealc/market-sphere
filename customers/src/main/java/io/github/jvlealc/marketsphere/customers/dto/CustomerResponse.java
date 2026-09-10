package io.github.jvlealc.marketsphere.customers.dto;

public record CustomerResponse(
        Long id,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        AddressResponse address,
        boolean active
) {
}

