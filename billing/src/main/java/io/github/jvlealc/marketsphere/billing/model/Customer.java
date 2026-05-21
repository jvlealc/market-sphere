package io.github.jvlealc.marketsphere.billing.model;

public record Customer(
        Long id,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        Address address
) { }
