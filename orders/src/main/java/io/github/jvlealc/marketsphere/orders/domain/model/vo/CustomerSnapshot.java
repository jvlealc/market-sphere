package io.github.jvlealc.marketsphere.orders.domain.model.vo;

import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidCustomerSnapshotException;

/**
 * Dados do cliente como estavam no momento da compra.
 */
public record CustomerSnapshot(
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

    public CustomerSnapshot {
        fullName = required(fullName, "Customer full name");
        nationalId = required(nationalId, "Customer national ID");
        email = required(email, "Customer email");
        phoneNumber = required(phoneNumber, "Customer phone number");
        postalCode = required(postalCode, "Customer postal code");
        street = required(street, "Customer street");
        houseNumber = required(houseNumber, "Customer house number");
        complement = optional(complement);
        neighborhood = optional(neighborhood);
        city = required(city, "Customer city");
        state = required(state, "Customer state");
        country = required(country, "Customer country");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidCustomerSnapshotException(fieldName + " is required");
        }

        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
