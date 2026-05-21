package io.github.jvlealc.marketsphere.shipping.messaging.subscriber;

public record OrderBilledCustomerPayload(
        Long customerId,
        String fullName,
        String email
) {
}
