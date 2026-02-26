package io.github.jvlealc.marketsphere.billing.subscriber.event;

public record CustomerPayload(
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
        String country
) { }
