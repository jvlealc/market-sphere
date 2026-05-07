package io.github.jvlealc.marketsphere.orders.messaging.publisher.event;

import io.github.jvlealc.marketsphere.orders.client.customers.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.order.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPaidEvent(
        Long orderId,
        CustomerRepresentation customer,
        Instant orderDate,
        BigDecimal orderTotal,
        OrderStatus orderStatus,
        String orderObservations,
        List<OrderItemPayload> orderItems
) { }
