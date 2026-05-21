package io.github.jvlealc.marketsphere.orders.application.ports.out;

import io.github.jvlealc.marketsphere.orders.application.output.OrderSummaryOutput;

import java.util.Optional;

public interface OrderQueryPort {
    Optional<OrderSummaryOutput> findOrderSummaryById(Long orderId);
}
