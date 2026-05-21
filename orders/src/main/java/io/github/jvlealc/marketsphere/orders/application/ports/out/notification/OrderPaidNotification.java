package io.github.jvlealc.marketsphere.orders.application.ports.out.notification;

import java.math.BigDecimal;

public record OrderPaidNotification(
    Long orderId,
    BigDecimal orderTotal,
    OrderPaidCustomerNotification customer
) {
}
