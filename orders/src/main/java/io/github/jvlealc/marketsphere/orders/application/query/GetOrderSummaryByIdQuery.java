package io.github.jvlealc.marketsphere.orders.application.query;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;

public record GetOrderSummaryByIdQuery(Long orderId) {

    public GetOrderSummaryByIdQuery {
        if (orderId == null) {
            throw new InvalidCommandException("Order ID must not be null");
        }

        if (orderId <= 0L) {
            throw new InvalidCommandException("Order ID must be greater than zero");
        }
    }
}
