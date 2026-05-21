package io.github.jvlealc.marketsphere.billing.subscriber.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPaidEvent(
        Long orderId,
        OrderPaidCustomerPayload customer,
        Instant orderDate,
        String orderObservations,
        BigDecimal orderTotal,
        List<OrderItemPayload> orderItems
) { }
