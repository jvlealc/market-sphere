package io.github.jvlealc.marketsphere.billing.application.outbox.payload;

public sealed interface OutboxPayload permits OrderBilledPayload {
}
