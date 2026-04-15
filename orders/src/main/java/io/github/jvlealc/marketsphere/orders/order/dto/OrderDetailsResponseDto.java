package io.github.jvlealc.marketsphere.orders.order.dto;

import io.github.jvlealc.marketsphere.orders.client.customers.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.order.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailsResponseDto(
        Long orderId,
        CustomerRepresentation customer,
        Instant orderDate,
        Instant paidAt,
        Instant billedAt,
        Instant shippedAt,
        BigDecimal orderTotal,
        OrderStatus orderStatus,
        String orderObservations,
        String invoiceUrl,
        UUID trackingCode,
        List<OrderItemDetailsResponseDto> orderItems
) { }
