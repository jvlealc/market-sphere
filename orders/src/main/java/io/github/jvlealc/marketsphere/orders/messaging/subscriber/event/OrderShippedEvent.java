package io.github.jvlealc.marketsphere.orders.messaging.subscriber.event;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
        Long orderId,
        UUID trackingCode,
        Instant shippedAt
) { }
