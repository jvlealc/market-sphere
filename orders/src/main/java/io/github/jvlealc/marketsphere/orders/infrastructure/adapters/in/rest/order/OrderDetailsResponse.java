package io.github.jvlealc.marketsphere.orders.interfaces.rest.order;

import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        String invoiceUrl,
        UUID trackingCode,
        List<OrderItemDetailsResponse> orderItems
) { }
