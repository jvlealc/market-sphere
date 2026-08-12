package io.github.jvlealc.marketsphere.orders.application.model.notification;

import java.math.BigDecimal;

public record OrderPaidNotification(
    Long orderId,
    BigDecimal orderTotal,
    OrderPaidCustomerNotification customer
) {
}
