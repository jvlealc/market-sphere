package io.github.jvlealc.marketsphere.orders.repository;

import io.github.jvlealc.marketsphere.orders.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
