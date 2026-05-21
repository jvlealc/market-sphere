package io.github.jvlealc.marketsphere.shipping.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public record DispatchShipmentRequest(
        UUID shipmentId,
        Long orderId,

        @NotBlank(message = "Tracking code is required")
        String trackingCode,

        @NotBlank(message = "Carrier is required")
        String carrier,

        Instant shippedAt
) {
        @AssertTrue(message = "Shipment ID or order ID is required")
        public boolean hasShipmentIdentifier() {
            return shipmentId != null || orderId != null;
        }
}
