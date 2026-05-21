package io.github.jvlealc.marketsphere.orders.domain.model.vo;

import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidCancellationRuleException;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.CancellationInitiator;

import java.time.Instant;
import java.util.Objects;

public class CancellationInfo {

    private final CancellationInitiator initiator;
    private final String reason;
    private final Instant canceledAt;

    // Construtor de criação e reconstituição
    private CancellationInfo(CancellationInitiator initiator, String reason, Instant canceledAt) {
        validateInvariants(initiator, reason, canceledAt);
        this.reason = normalizeReason(reason);
        this.initiator = initiator;
        this.canceledAt = canceledAt;
    }

    // Factory method de criação
    public static CancellationInfo createNew(CancellationInitiator initiator, String reason) {
        return new CancellationInfo(initiator, reason,  Instant.now());
    }

    // Factory method de reconstituição
    public static CancellationInfo rehydrate(CancellationInitiator initiator, String reason, Instant canceledAt) {
        return new CancellationInfo(initiator, reason, canceledAt);
    }

    public CancellationInitiator getInitiator() {
        return initiator;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

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

    private static void validateInvariants(CancellationInitiator initiator, String reason, Instant canceledAt) {
        if (initiator == null) {
            throw new InvalidCancellationRuleException("The author of the cancellation must not be null");
        }

        if (canceledAt == null) {
            throw new InvalidCancellationRuleException("The cancellation date is required for reconstitution");
        }

        boolean isReasonRequired = initiator == CancellationInitiator.ADMIN
                                || initiator == CancellationInitiator.SYSTEM
                                || initiator == CancellationInitiator.MERCHANT;

        if (isReasonRequired && (reason == null  || reason.isBlank())) {
            throw new InvalidCancellationRuleException("If the cancellation initiator is SYSTEM, ADMIN, or MERCHANT, the cancellation reason is required");
        }
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason;
    }
}
