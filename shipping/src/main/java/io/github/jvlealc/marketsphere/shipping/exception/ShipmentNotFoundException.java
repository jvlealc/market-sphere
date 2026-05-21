package io.github.jvlealc.marketsphere.shipping.exception;

import java.util.UUID;

public class ShipmentNotFoundException extends ApplicationException {

    public ShipmentNotFoundException(UUID shipmentId) {
        super("Not found shipment with ID: " + shipmentId);
    }

    public ShipmentNotFoundException(Long orderId) {
        super("Not found shipment with order ID: " + orderId);
    }
}
