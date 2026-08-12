package io.github.jvlealc.marketsphere.orders.infrastructure.persistence.mapper;

import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import io.github.jvlealc.marketsphere.orders.domain.model.OrderItem;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.CancellationInfoJpaEntity;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.OrderItemJpaEntity;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.OrderJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OrderJpaEntityMapper {

    private final OrderItemJpaEntityMapper orderItemMapper;
    private final PaymentInfoJpaEntityMapper paymentInfoMapper;
    private final CancellationInfoJpaEntityMapper cancellationInfoMapper;

    public OrderJpaEntity toNewEntity(Order order) {
        Objects.requireNonNull(order, "Order must not be null");

        OrderJpaEntity entity = new OrderJpaEntity();

        copyCreationProperties(order, entity);
        entity.setPaymentInfo(paymentInfoMapper.toNewEntity(order.getPaymentInfo()));
        entity.setOrderItems(toNewOrderItemEntities(order.getOrderItems()));

        return entity;
    }

    public Order toDomain(OrderJpaEntity entity) {
        Objects.requireNonNull(entity, "Entity must not be null");
        return Order.rehydrate(
                entity.getId(),
                entity.getCustomerId(),
                entity.getOrderDate(),
                entity.getPaidAt(),
                entity.getBilledAt(),
                entity.getShippedAt(),
                entity.getPaymentKey(),
                entity.getObservations(),
                entity.getStatus(),
                entity.getTotal(),
                entity.getTrackingCode(),
                entity.getInvoiceUrl(),
                paymentInfoMapper.toDomain(entity.getPaymentInfo()),
                toOrderItemDomains(entity.getOrderItems()),
                cancellationInfoMapper.toDomain(entity.getCancellationInfo())
        );
    }

    public void copyStateToExistingEntity(Order order, OrderJpaEntity entity) {
        Objects.requireNonNull(order, "Order must not be null");
        Objects.requireNonNull(entity, "Entity must not be null");

        copyUpdatableProperties(order, entity);
        attachCancellationInfoIfCreated(order, entity);
    }

    private static void copyCreationProperties(Order source, OrderJpaEntity target) {
        target.setCustomerId(source.getCustomerId());
        target.setOrderDate(source.getOrderDate());
        target.setPaymentKey(source.getPaymentKey());
        target.setObservations(source.getObservations());
        target.setStatus(source.getStatus());
        target.setTotal(source.getTotal());
    }

    private static void copyUpdatableProperties(Order source, OrderJpaEntity target) {
        target.setPaidAt(source.getPaidAt());
        target.setBilledAt(source.getBilledAt());
        target.setShippedAt(source.getShippedAt());
        target.setPaymentKey(source.getPaymentKey());
        target.setObservations(source.getObservations());
        target.setStatus(source.getStatus());
        target.setTrackingCode(source.getTrackingCode());
        target.setInvoiceUrl(source.getInvoiceUrl());
    }

    private void attachCancellationInfoIfCreated(Order order, OrderJpaEntity entity) {
        if (order.getCancellationInfo() == null) return;
        if (entity.getCancellationInfo() != null) return;

        CancellationInfoJpaEntity cancellationEntity =
                cancellationInfoMapper.toNewEntity(order.getCancellationInfo());

        entity.setCancellationInfo(cancellationEntity);
    }

    private List<OrderItemJpaEntity> toNewOrderItemEntities(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return List.of();
        }
        return orderItems.stream()
                .map(orderItemMapper::toNewEntity)
                .toList();
    }

    private List<OrderItem> toOrderItemDomains(List<OrderItemJpaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(orderItemMapper::toDomain)
                .toList();
    }
}
