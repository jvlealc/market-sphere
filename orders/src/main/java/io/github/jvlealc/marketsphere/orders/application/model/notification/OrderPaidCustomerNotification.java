package io.github.jvlealc.marketsphere.orders.application.model.notification;

public record OrderPaidCustomerNotification(
        Long id,
        String fullName,
        String email
) {
}
