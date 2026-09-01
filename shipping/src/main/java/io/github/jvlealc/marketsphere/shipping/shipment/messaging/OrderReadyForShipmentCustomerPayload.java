package io.github.jvlealc.marketsphere.shipping.shipment.messaging;

public record OrderReadyForShipmentCustomerPayload(
        Long customerId,
        String fullName,
        String email
) {
}
