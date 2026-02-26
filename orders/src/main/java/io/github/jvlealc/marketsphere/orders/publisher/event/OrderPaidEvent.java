package io.github.jvlealc.marketsphere.orders.publisher.event;

import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.model.enums.OrderStatus;

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
