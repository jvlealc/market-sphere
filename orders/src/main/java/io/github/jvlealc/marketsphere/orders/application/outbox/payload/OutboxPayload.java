package io.github.jvlealc.marketsphere.orders.application.outbox.payload;

public sealed interface OutboxPayload
        permits OrderPaidMessagingPayload, OrderPaidNotificationPayload, OrderReadyForShipmentPayload,
        PaymentRequestPayload {
}
