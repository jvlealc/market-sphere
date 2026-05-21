package io.github.jvlealc.marketsphere.shipping.messaging.publisher;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
        Long orderId,
        String trackingCode,
        Instant shippedAt
) { }
