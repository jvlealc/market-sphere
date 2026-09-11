package io.github.jvlealc.marketsphere.billing.application.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    DEAD
}
