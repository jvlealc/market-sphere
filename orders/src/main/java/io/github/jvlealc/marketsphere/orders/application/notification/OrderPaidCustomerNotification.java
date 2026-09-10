package io.github.jvlealc.marketsphere.orders.application.notification;

public record OrderPaidCustomerNotification(
        Long id,
        String fullName,
        String email
) {
}
