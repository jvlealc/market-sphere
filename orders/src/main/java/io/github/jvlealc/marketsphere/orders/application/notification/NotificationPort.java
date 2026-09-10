package io.github.jvlealc.marketsphere.orders.application.notification;

public interface NotificationPort {

    void sendPaidOrderConfirmation(OrderPaidNotification notification);
}
