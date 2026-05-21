package io.github.jvlealc.marketsphere.orders.application.ports.out;

import io.github.jvlealc.marketsphere.orders.domain.model.Order;

import java.util.Optional;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(Long orderId);

    Optional<Order> findWithDetailsById(Long orderId);

    Optional<Order> findByIdAndPaymentKey(Long orderId, String paymentKey);
}
