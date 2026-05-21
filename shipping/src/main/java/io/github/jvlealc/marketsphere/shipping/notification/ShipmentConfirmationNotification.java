package io.github.jvlealc.marketsphere.shipping.notification;

import java.time.Instant;

public record ShipmentConfirmationNotification(
        Long orderId,
        String customerName,
        String customerEmail,
        String trackingCode,
        Instant shippedAt
) {
}
