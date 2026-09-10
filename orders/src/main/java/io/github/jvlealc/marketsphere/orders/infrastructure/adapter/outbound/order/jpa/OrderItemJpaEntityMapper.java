package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.order.jpa;

import io.github.jvlealc.marketsphere.orders.domain.order.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemJpaEntityMapper {

    @Mapping(target = "order", ignore = true)
    OrderItemJpaEntity toNewEntity(OrderItem orderItem);

    default OrderItem toDomain(OrderItemJpaEntity entity) {
        if (entity == null) return null;
        return OrderItem.rehydrate(entity.getId(), entity.getProductId(), entity.getProductName(), entity.getAmount(), entity.getUnitPrice());
    }
}
