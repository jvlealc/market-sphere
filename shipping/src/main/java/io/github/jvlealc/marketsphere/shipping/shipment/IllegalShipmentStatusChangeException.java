package io.github.jvlealc.marketsphere.shipping.shipment;

public class IllegalShipmentStatusChangeException extends RuntimeException {

    public IllegalShipmentStatusChangeException(String message) {
        super(message);
    }
}
