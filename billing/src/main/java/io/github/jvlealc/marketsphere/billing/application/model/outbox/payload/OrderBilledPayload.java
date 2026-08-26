package io.github.jvlealc.marketsphere.billing.application.model.outbox.payload;

import java.util.UUID;

public sealed interface OrderBilledPayload extends OutboxPayload
        permits OrderBilledMessagingPayload, OrderBilledEmailPayload {

    Long orderId();
    UUID invoiceId();
}
