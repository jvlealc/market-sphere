package io.github.jvlealc.marketsphere.shipping.event;

import java.time.Instant;
import java.util.UUID;

public record ShipmentDispatchedApplicationEvent(
        UUID shipmentId,
        Long orderId,
        String trackingCode,
        Instant shippedAt
) {
}
