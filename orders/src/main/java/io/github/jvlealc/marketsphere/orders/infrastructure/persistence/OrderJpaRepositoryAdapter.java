package io.github.jvlealc.marketsphere.orders.infrastructure.persistence;

import io.github.jvlealc.marketsphere.orders.application.output.OrderSummaryOutput;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderQueryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;
import io.github.jvlealc.marketsphere.orders.infrastructure.exception.OrderPersistenceException;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.OrderJpaEntity;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.mapper.OrderJpaEntityMapper;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.projection.OrderSummaryJpaProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderJpaRepositoryAdapter implements OrderRepositoryPort, OrderQueryPort {

    private final SpringDataOrderRepository springDataOrderRepository;
    private final OrderJpaEntityMapper orderJpaEntityMapper;

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            OrderJpaEntity saved = springDataOrderRepository.save(
                    orderJpaEntityMapper.toNewEntity(order)
            );
          return orderJpaEntityMapper.toDomain(saved);
        }

        OrderJpaEntity existingEntity = springDataOrderRepository.findWithDetailsById(order.getId())
                .orElseThrow(() -> new OrderPersistenceException("Not found order entity while trying update order with ID " + order.getId()));

        orderJpaEntityMapper.copyStateToExistingEntity(order, existingEntity);
        springDataOrderRepository.save(existingEntity);

        return orderJpaEntityMapper.toDomain(existingEntity);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return springDataOrderRepository.findById(orderId)
                .map(orderJpaEntityMapper::toDomain);
    }

    @Override
    public Optional<Order> findWithDetailsById(Long orderId) {
        return springDataOrderRepository.findWithDetailsById(orderId)
                .map(orderJpaEntityMapper::toDomain);
    }

    @Override
    public Optional<Order> findByIdAndPaymentKey(Long orderId, String paymentKey) {
        return springDataOrderRepository.findByIdAndPaymentKey(orderId, paymentKey)
                .map(orderJpaEntityMapper::toDomain);
    }

    @Override
    public Optional<OrderSummaryOutput> findOrderSummaryById(Long orderId) {
        return springDataOrderRepository.findSummaryById(orderId)
                .map(this::toSummaryOutput);
    }

    private OrderSummaryOutput toSummaryOutput(OrderSummaryJpaProjection projection) {
        return new OrderSummaryOutput(
                projection.getId(),
                projection.getCustomerId(),
                projection.getOrderDate(),
                projection.getObservations(),
                OrderStatus.valueOf(projection.getStatus()),
                projection.getTotal(),
                projection.getAmountItems()
        );
    }
}
