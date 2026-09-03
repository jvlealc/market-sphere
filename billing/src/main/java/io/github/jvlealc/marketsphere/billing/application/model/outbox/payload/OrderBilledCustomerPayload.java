package io.github.jvlealc.marketsphere.billing.application.model.outbox.payload;

public record OrderBilledCustomerPayload(
        Long customerId,
        String fullName,
        String email
) {
    public OrderBilledCustomerPayload {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("customerId must not be null and must be greater than zero");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be null or blank");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be null or blank");
        }

        fullName = fullName.trim();
        email = email.trim();
    }
}
