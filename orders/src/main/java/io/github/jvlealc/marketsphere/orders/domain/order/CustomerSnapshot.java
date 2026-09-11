package io.github.jvlealc.marketsphere.orders.domain.order;

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
        fullName = required(fullName, "fullName");
        nationalId = required(nationalId, "nationalId");
        email = required(email, "email");
        phoneNumber = required(phoneNumber, "phoneNumber");
        postalCode = required(postalCode, "postalCode");
        street = required(street, "street");
        houseNumber = required(houseNumber, "houseNumber");
        complement = optional(complement);
        neighborhood = optional(neighborhood);
        city = required(city, "city");
        state = required(state, "state");
        country = required(country, "country");
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
