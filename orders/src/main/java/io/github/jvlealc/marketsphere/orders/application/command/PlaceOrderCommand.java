package io.github.jvlealc.marketsphere.orders.application.command;

import java.util.List;

public record PlaceOrderCommand(
        Long customerId,
        PaymentInfoCommand paymentInfo,
        List<OrderItemCommand> orderItems
) {
}
