package io.github.jvlealc.marketsphere.billing.application.order;

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
            throw new InvalidOrderPaidSnapshotException("customerId is required");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("fullName is required");
        }

        if (nationalId == null || nationalId.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("nationalId is required");
        }

        if (email == null || email.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("email is required");
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("phoneNumber is required");
        }

        if (address == null) {
            throw new InvalidOrderPaidSnapshotException("address is required");
        }
    }
}
