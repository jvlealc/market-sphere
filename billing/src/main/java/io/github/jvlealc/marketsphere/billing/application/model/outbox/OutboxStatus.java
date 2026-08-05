package io.github.jvlealc.marketsphere.billing.application.model.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    DEAD
}
