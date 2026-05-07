package io.github.jvlealc.marketsphere.orders.messaging.publisher.mapper;

import io.github.jvlealc.marketsphere.orders.client.customers.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.order.model.Order;
import io.github.jvlealc.marketsphere.orders.messaging.publisher.event.OrderItemPayload;
import io.github.jvlealc.marketsphere.orders.messaging.publisher.event.OrderPaidEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderPaidEventMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.orderDate", target = "orderDate", dateFormat = "yyyy-MM-dd")
    @Mapping(source = "order.total", target = "orderTotal")
    @Mapping(source = "order.status", target = "orderStatus")
    @Mapping(source = "order.observations", target = "orderObservations")
    @Mapping(source = "customerRepresentation", target = "customer")
    @Mapping(source = "orderItemRepresentations", target = "orderItems")
    OrderPaidEvent toOrderEvent(
            Order order,
            CustomerRepresentation customerRepresentation,
            List<OrderItemPayload> orderItemRepresentations
    );
}
