package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.rest.order;

import io.github.jvlealc.marketsphere.orders.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailsResponse(
        Long orderId,
        OrderCustomerResponse customer,
        Instant orderDate,
        Instant paidAt,
        Instant billedAt,
        Instant shippedAt,
        BigDecimal orderTotal,
        OrderStatus orderStatus,
        String orderObservations,
        String invoiceId,
        String trackingCode,
        List<OrderItemDetailsResponse> orderItems
) { }
