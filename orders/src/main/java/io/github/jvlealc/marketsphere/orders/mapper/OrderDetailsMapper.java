package io.github.jvlealc.marketsphere.orders.mapper;

import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.dto.OrderDetailsResponseDto;
import io.github.jvlealc.marketsphere.orders.dto.OrderItemDetailsResponseDto;
import io.github.jvlealc.marketsphere.orders.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderDetailsMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.orderDate", target = "orderDate", dateFormat = "yyyy-MM-dd")
    @Mapping(source = "order.paidAt", target = "paidAt")
    @Mapping(source = "order.billedAt", target = "billedAt")
    @Mapping(source = "order.shippedAt", target = "shippedAt")
    @Mapping(source = "order.total", target = "orderTotal")
    @Mapping(source = "order.status", target = "orderStatus")
    @Mapping(source = "order.observations", target = "orderObservations")
    @Mapping(source = "order.invoiceUrl", target = "invoiceUrl")
    @Mapping(source = "order.trackingCode", target = "trackingCode")
    @Mapping(source = "customerRepresentation", target = "customer")
    @Mapping(source = "orderItemDtos", target = "orderItems")
    OrderDetailsResponseDto toOrderDetailsDto(
            Order order,
            CustomerRepresentation customerRepresentation,
            List<OrderItemDetailsResponseDto> orderItemDtos
    );
}
