package io.github.jvlealc.marketsphere.orders.application.query;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidQueryException;

public record GetOrderSummaryByIdQuery(Long orderId) {

    public GetOrderSummaryByIdQuery {
        if (orderId == null) {
            throw new InvalidQueryException("Order ID must not be null");
        }

        if (orderId <= 0L) {
            throw new InvalidQueryException("Order ID must be greater than zero");
        }
    }
}
