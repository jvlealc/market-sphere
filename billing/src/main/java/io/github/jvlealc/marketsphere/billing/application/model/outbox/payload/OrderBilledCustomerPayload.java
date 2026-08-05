package io.github.jvlealc.marketsphere.billing.application.model.outbox.payload;

public record OrderBilledCustomerPayload(
        Long customerId,
        String fullName,
        String email
) {
    public OrderBilledCustomerPayload {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Customer ID is required and must be positive");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Customer email is required");
        }

        fullName = fullName.trim();
        email = email.trim();
    }
}
