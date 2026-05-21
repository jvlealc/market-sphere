package io.github.jvlealc.marketsphere.billing.publisher.event;

import java.time.Instant;

public record OrderBilledEvent(
        Long orderId,
        String invoiceUrl,
        Instant billedAt,
        OrderBilledCustomerPayload customer
) { }
