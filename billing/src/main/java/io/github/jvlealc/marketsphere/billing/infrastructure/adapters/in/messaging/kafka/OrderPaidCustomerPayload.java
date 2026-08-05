package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.messaging.kafka;

public record OrderPaidCustomerPayload(
        Long customerId,
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
) { }
