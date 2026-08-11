package io.github.jvlealc.marketsphere.orders.application.ports.out.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    DEAD
}
