package io.github.jvlealc.marketsphere.orders.publisher.mapper;

import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.model.OrderItem;
import io.github.jvlealc.marketsphere.orders.publisher.event.OrderItemPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemPayloadMapper {

    @Mapping(source = "orderItem.productId", target = "productId")
    @Mapping(source = "orderItem.amount", target = "amount")
    @Mapping(source = "orderItem.unitPrice", target = "unitPrice")
    @Mapping(source = "productRepresentation.name", target = "productName")
    OrderItemPayload toOrderItemPayload(OrderItem orderItem, ProductRepresentation productRepresentation);
}
