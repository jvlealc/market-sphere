package io.github.jvlealc.marketsphere.orders.application.ports.out;

import io.github.jvlealc.marketsphere.orders.application.model.notification.OrderPaidNotification;

public interface NotificationPort {

    void sendPaidOrderConfirmation(OrderPaidNotification notification);
}
