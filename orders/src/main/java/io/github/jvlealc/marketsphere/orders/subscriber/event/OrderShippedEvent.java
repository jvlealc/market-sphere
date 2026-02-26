package io.github.jvlealc.marketsphere.orders.subscriber.event;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
        Long orderId,
        UUID trackingCode,
        Instant shippedAt
) { }
