package io.github.jvlealc.marketsphere.orders.domain.model.vo;

import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidCancellationRuleException;
import io.github.jvlealc.marketsphere.orders.domain.exception.OrderDomainException;
import io.github.jvlealc.marketsphere.orders.domain.exception.OrderRehydrationException;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.CancellationInitiator;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

public class CancellationInfo {

    private final CancellationInitiator initiator;
    private final String reason;
    private final Instant canceledAt;

    // Construtor de criação e reconstituição
    private CancellationInfo(CancellationInitiator initiator, String reason, Instant canceledAt) {
        this.reason = normalizeReason(reason);
        this.initiator = initiator;
        this.canceledAt = canceledAt;
    }

    // Factory method de criação
    public static CancellationInfo createNew(CancellationInitiator initiator, String reason) {
        validateCreationInvariants(initiator, reason);
        return new CancellationInfo(initiator, reason,  Instant.now());
    }

    // Factory method de reconstituição
    public static CancellationInfo rehydrate(CancellationInitiator initiator, String reason, Instant canceledAt) {
        validateRehydrationInvariants(initiator, reason, canceledAt);
        return new CancellationInfo(initiator, reason, canceledAt);
    }

    public CancellationInitiator getInitiator() { return initiator; }
    public String getReason() { return reason; }
    public Instant getCanceledAt() { return canceledAt; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CancellationInfo that = (CancellationInfo) o;
        return initiator == that.initiator && Objects.equals(reason, that.reason) && Objects.equals(canceledAt, that.canceledAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initiator, reason, canceledAt);
    }

    private static void validateCreationInvariants(CancellationInitiator initiator, String reason) {
        if (initiator == null) {
            throw new InvalidCancellationRuleException("The initiator of the cancellation must not be null");
        }

        validateReasonIfRequired(initiator, reason, InvalidCancellationRuleException::new);
    }

    private static void validateRehydrationInvariants(CancellationInitiator initiator, String reason, Instant canceledAt) {
        if (initiator == null) {
            throw new OrderRehydrationException("Rehydrated cancellation info must have an initiator");
        }

        if (canceledAt == null) {
            throw new OrderRehydrationException("Rehydrated cancellation info must have a cancellation date");
        }

        validateReasonIfRequired(initiator, reason, OrderRehydrationException::new);
    }

    private static void validateReasonIfRequired(
            CancellationInitiator initiator,
            String reason,
            Function<String, OrderDomainException> exceptionFactory
    ) {
        boolean isReasonRequired = initiator == CancellationInitiator.ADMIN
                || initiator == CancellationInitiator.SYSTEM
                || initiator == CancellationInitiator.MERCHANT;

        if (isReasonRequired && (reason == null  || reason.isBlank())) {
            throw exceptionFactory.apply("If the cancellation initiator is SYSTEM, ADMIN, or MERCHANT, the reason is required");
        }
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason;
    }
}
