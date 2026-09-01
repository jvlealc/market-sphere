package io.github.jvlealc.marketsphere.shipping.shipment;

import java.util.UUID;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(UUID shipmentId) {
        super("Not found shipment with ID: " + shipmentId);
    }

    public ShipmentNotFoundException(Long orderId) {
        super("Not found shipment with order ID: " + orderId);
    }
}
