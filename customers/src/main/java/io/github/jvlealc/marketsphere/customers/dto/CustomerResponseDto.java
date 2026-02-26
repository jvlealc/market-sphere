package io.github.jvlealc.marketsphere.customers.dto;

public record CustomerResponseDto(
        Long id,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        String postalCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String country,
        boolean active
) { }

