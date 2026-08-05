package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.messaging.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPaidEvent(
        Long orderId,
        OrderPaidCustomerPayload customer,
        Instant orderDate,
        String orderObservations,
        BigDecimal orderTotal,
        List<OrderPaidItemPayload> orderItems
) { }
