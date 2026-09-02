package io.github.jvlealc.marketsphere.shipping.shipment.rest;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record DispatchShipmentRequest(
        UUID shipmentId,
        Long orderId,

        @NotBlank(message = "Tracking code is required")
        @Size(max = 120, message = "Tracking code must not exceed 120 characters")
        String trackingCode,

        @NotBlank(message = "Carrier is required")
        @Size(max = 100, message = "Carrier must not exceed 100 characters")
        String carrier,

        @PastOrPresent(message = "Shipped at date must not be in the future")
        Instant shippedAt
) {
        @AssertTrue(message = "Shipment ID or order ID is required")
        public boolean hasShipmentIdentifier() {
            return shipmentId != null || orderId != null;
        }
}
