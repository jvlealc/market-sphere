package io.github.jvlealc.marketsphere.orders.application.order.placement;

import io.github.jvlealc.marketsphere.orders.application.InvalidCommandException;

import java.util.List;

public record PlaceOrderCommand(
        Long customerId,
        PaymentInfoCommand paymentInfo,
        List<OrderItemCommand> orderItems
) {
    public PlaceOrderCommand {
        if (customerId == null) {
            throw new InvalidCommandException("customerId must not be null");
        }

        if (customerId <= 0L) {
            throw new InvalidCommandException("customerId must be greater than zero");
        }

        if (paymentInfo == null) {
            throw new InvalidCommandException("paymentInfo must not be null");
        }

        if (orderItems == null || orderItems.isEmpty()) {
            throw new InvalidCommandException("orderItems must not be empty");
        }
    }
}
