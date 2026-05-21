package io.github.jvlealc.marketsphere.orders.application.support;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidOutboxMessageException;

public final class OutboxAggregateIdsParser {

    private OutboxAggregateIdsParser() {
    }

    public static Long parseOrderId(String aggregateId) {
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new InvalidOutboxMessageException("Outbox aggregate ID is required");
        }

        try {
            return Long.valueOf(aggregateId);
        }  catch (NumberFormatException e) {
            throw new InvalidOutboxMessageException("Invalid order ID in outbox aggregateId: " + aggregateId, e);
        }
    }
}
