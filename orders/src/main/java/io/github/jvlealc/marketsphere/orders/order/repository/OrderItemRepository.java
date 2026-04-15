package io.github.jvlealc.marketsphere.orders.order.repository;

import io.github.jvlealc.marketsphere.orders.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
