package io.github.jvlealc.marketsphere.billing.publisher.event;

public record OrderBilledCustomerPayload(
        Long customerId,
        String fullName,
        String email
) {
}
