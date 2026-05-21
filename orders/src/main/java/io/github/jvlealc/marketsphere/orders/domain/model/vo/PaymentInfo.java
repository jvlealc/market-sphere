package io.github.jvlealc.marketsphere.orders.domain.model.vo;

import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidPaymentInfoException;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.PaymentType;

import java.time.Instant;
import java.util.Objects;

public class PaymentInfo {
    private final String metadata;
    private final PaymentType paymentType;
    private final Instant createdAt;

    private PaymentInfo(String metadata, PaymentType paymentType,  Instant createdAt) {
        validateInvariants(paymentType, createdAt);
        this.metadata = normalizeMetadata(metadata);
        this.paymentType = paymentType;
        this.createdAt = createdAt;
    }

    public static PaymentInfo createNew(String metadata, PaymentType paymentType) {
        return new PaymentInfo(metadata, paymentType, Instant.now());
    }

    public static PaymentInfo rehydrate(String metadata, PaymentType paymentType, Instant createdAt) {
        return new PaymentInfo(metadata, paymentType, createdAt);
    }

    public String getMetadata() { return metadata; }
    public PaymentType getPaymentType() { return paymentType; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PaymentInfo that = (PaymentInfo) obj;
        return Objects.equals(metadata, that.metadata) && paymentType == that.paymentType && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, paymentType, createdAt);
    }

    private static void validateInvariants(PaymentType paymentType, Instant createdAt) {
        if (paymentType == null) {
            throw new InvalidPaymentInfoException("Payment type is required");
        }
        if (createdAt == null) {
            throw new InvalidPaymentInfoException("Payment creation date is required");
        }
    }

    private static String normalizeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        return metadata.trim();
    }
}
