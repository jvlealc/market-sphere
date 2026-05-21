package io.github.jvlealc.marketsphere.shipping.messaging.subscriber;

import java.time.Instant;

public record OrderBilledEvent(
        Long orderId,
        Instant billedAt,
        OrderBilledCustomerPayload customer
) {
}
