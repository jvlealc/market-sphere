package io.github.jvlealc.marketsphere.orders.application.ports.out.notification;

public record OrderPaidCustomerNotification(
        Long id,
        String fullName,
        String email
) {
}
