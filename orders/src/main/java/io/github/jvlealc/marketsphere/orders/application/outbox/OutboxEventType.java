package io.github.jvlealc.marketsphere.orders.application.outbox;

public enum OutboxEventType {
    PAYMENT_REQUEST_REQUIRED,
    ORDER_PAID,
    ORDER_READY_FOR_SHIPMENT
}
