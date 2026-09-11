package io.github.jvlealc.marketsphere.orders.application.order.placement;

import io.github.jvlealc.marketsphere.orders.application.InvalidCommandException;

public record OrderItemCommand(Long productId, Integer amount) {

    public OrderItemCommand {
        if (productId == null) {
            throw new InvalidCommandException("productId must not be null");
        }

        if (productId <= 0L) {
            throw new InvalidCommandException("productId must be greater than zero");
        }

        if (amount == null) {
            throw new InvalidCommandException("amount must not be null");
        }

        if (amount <= 0) {
            throw new InvalidCommandException("amount must be greater than zero");
        }
    }
}
