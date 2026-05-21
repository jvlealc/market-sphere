package io.github.jvlealc.marketsphere.orders.domain.model.enums;

public enum OrderStatus {
    PAYMENT_PENDING,
    PAYMENT_ERROR,
    PAID,
    BILLED,
    PREPARING_SHIPMENT,
    SHIPPED,
    CANCELED
}
