package io.github.jvlealc.marketsphere.orders.application.command;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;

import java.util.List;

public record PlaceOrderCommand(
        Long customerId,
        PaymentInfoCommand paymentInfo,
        List<OrderItemCommand> orderItems
) {
    public PlaceOrderCommand {
        if (customerId == null) {
            throw new InvalidCommandException("Customer ID must not be null");
        }

        if (customerId <= 0L) {
            throw new InvalidCommandException("Customer ID must be greater than zero");
        }

        if (paymentInfo == null) {
            throw new InvalidCommandException("Payment Info must not be null");
        }

        if (orderItems == null || orderItems.isEmpty()) {
            throw new InvalidCommandException("Order Items must not be empty");
        }
    }
}
