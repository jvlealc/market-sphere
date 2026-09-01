package io.github.jvlealc.marketsphere.shipping.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    DEAD
}
