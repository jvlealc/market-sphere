package io.github.jvlealc.marketsphere.billing.application.outbox.payload;

import java.util.UUID;

public sealed interface OrderBilledPayload extends OutboxPayload
        permits OrderBilledMessagingPayload, OrderBilledEmailPayload {

    Long orderId();
    UUID invoiceId();
}
