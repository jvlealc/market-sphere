package io.github.jvlealc.marketsphere.orders.application.order.query;

import java.util.Optional;

public interface OrderQueryPort {
    Optional<OrderSummaryOutput> findOrderSummaryById(Long orderId);
}
