package io.github.jvlealc.marketsphere.billing.application.model.order;

import io.github.jvlealc.marketsphere.billing.application.exception.InvalidOrderPaidSnapshotException;

public record OrderPaidCustomer(
        Long customerId,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        OrderPaidAddress address
) {
    public OrderPaidCustomer {
        if (customerId == null) {
            throw new InvalidOrderPaidSnapshotException("Customer ID is required");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("Customer name is required");
        }

        if (nationalId == null || nationalId.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("National ID is required");
        }

        if (email == null || email.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("Email is required");
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("Phone number is required");
        }

        if (address == null) {
            throw new InvalidOrderPaidSnapshotException("Customer address is required");
        }
    }
}
