package io.github.jvlealc.marketsphere.orders.application.order.shipping;

import io.github.jvlealc.marketsphere.orders.application.InvalidCommandException;

public record HandleOrderPreparingShipmentCommand(Long orderId) {

    public HandleOrderPreparingShipmentCommand {
        if (orderId == null) {
            throw new InvalidCommandException("Order ID must not be null");
        }

        if (orderId <= 0L) {
            throw new InvalidCommandException("Order ID must be greater than zero");
        }
    }
}
