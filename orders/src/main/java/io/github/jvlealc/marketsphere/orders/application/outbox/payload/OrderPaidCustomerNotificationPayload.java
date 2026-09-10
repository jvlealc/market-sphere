package io.github.jvlealc.marketsphere.orders.application.outbox.payload;

import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredId;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredText;

public record OrderPaidCustomerNotificationPayload(
        Long customerId,
        String fullName,
        String email
) {

    public OrderPaidCustomerNotificationPayload {
        customerId = requiredId(customerId, "Customer ID");
        fullName = requiredText(fullName, "Customer full name");
        email = requiredText(email, "Customer email");
    }
}
