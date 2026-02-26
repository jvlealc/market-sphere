package io.github.jvlealc.marketsphere.orders.mapper;

import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.dto.OrderItemDetailsResponseDto;
import io.github.jvlealc.marketsphere.orders.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemDetailsMapper {

    @Mapping(source = "orderItem.productId", target = "productId")
    @Mapping(source = "orderItem.amount", target = "amount")
    @Mapping(source = "orderItem.unitPrice", target = "unitPrice")
    @Mapping(source = "productRepresentation.name", target = "productName")
    @Mapping(source = "productRepresentation.active", target = "active")
    OrderItemDetailsResponseDto toOrderItemDetailsDto(OrderItem orderItem, ProductRepresentation productRepresentation);
}