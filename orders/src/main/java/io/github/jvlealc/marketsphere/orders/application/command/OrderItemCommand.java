package io.github.jvlealc.marketsphere.orders.application.command;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;

public record OrderItemCommand(Long productId, Integer amount) {

    public OrderItemCommand {
        if (productId == null) {
            throw new InvalidCommandException("Product ID must not be null");
        }

        if (productId <= 0L) {
            throw new InvalidCommandException("Product ID must be greater than zero");
        }

        if (amount <= 0) {
            throw new InvalidCommandException("Amount must be greater than zero");
        }
    }
}
