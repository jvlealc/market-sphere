package io.github.jvlealc.marketsphere.shipping.shipment.rest;

public class InvalidShipmentRequestException extends RuntimeException {

    public InvalidShipmentRequestException(String message) {
        super(message);
    }
}
