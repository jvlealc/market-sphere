package io.github.jvlealc.marketsphere.orders.application.ports.out.notification;

public interface NotificationPort {

    void sendPaidOrderConfirmation(OrderPaidNotification notification);
}
