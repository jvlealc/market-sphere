package io.github.jvlealc.marketsphere.orders.application.model.outbox.payload;

public sealed interface OutboxPayload
        permits OrderPaidMessagingPayload, OrderPaidNotificationPayload, OrderReadyForShipmentPayload,
        PaymentRequestPayload {
}
