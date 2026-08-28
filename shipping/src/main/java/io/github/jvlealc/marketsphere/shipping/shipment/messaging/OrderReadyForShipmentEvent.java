package io.github.jvlealc.marketsphere.shipping.shipment.messaging;

import java.time.Instant;

public record OrderReadyForShipmentEvent(
        Long orderId,
        Instant billedAt,
        OrderReadyForShipmentCustomerPayload customer
) {
}
