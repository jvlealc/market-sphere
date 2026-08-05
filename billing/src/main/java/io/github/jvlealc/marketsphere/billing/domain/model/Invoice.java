package io.github.jvlealc.marketsphere.billing.domain.model;

import io.github.jvlealc.marketsphere.billing.domain.exception.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class Invoice {

    private static final int MAX_FAILURE_REASON_LENGTH = 2_000;

    private final UUID id;
    private final Long orderId;
    private InvoiceStatus status;
    private String storageKey;
    private Instant generatedAt;
    private Instant failedAt;
    private String failureReason;

    private Invoice(UUID id, Long orderId, InvoiceStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
    }

    private Invoice(
            UUID id,
            Long orderId,
            InvoiceStatus status,
            String storageKey,
            Instant generatedAt,
            Instant failedAt,
            String failureReason
    ) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.storageKey = normalizeOptionalText(storageKey);
        this.generatedAt = generatedAt;
        this.failedAt = failedAt;
        this.failureReason = normalizeOptionalText(failureReason);
    }

    public static Invoice createNew(UUID id, Long orderId) {
        validateNewInvoice(id, orderId);
        return new Invoice(id, orderId, InvoiceStatus.PROCESSING);
    }

    public static Invoice rehydrate(
            UUID id,
            Long orderId,
            InvoiceStatus status,
            String storageKey,
            Instant generatedAt,
            Instant failedAt,
            String failureReason
    ) {
        validateRehydratedInvoice(id, orderId, status);
        validateRehydratedStatusConsistency(status, storageKey, generatedAt, failedAt, failureReason);

        return new Invoice(
                id,
                orderId,
                status,
                storageKey,
                generatedAt,
                failedAt,
                failureReason
        );
    }

    public UUID getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean markAsGenerated(String storageKey, Instant generatedAt) {
        throwIfAlreadyFailed();

        String normalizedStorageKey = requireNonBlank(storageKey, "Storage key");
        Instant requiredGeneratedAt = requireNonNull(generatedAt, "Generated at date");

        if (this.status == InvoiceStatus.GENERATED) {
            if (!this.storageKey.equals(normalizedStorageKey)) {
                throw new InvalidInvoiceStateException("Conflicting generation data received for an already generated invoice");
            }
            return false;
        }

        this.status = InvoiceStatus.GENERATED;
        this.storageKey = normalizedStorageKey;
        this.generatedAt = requiredGeneratedAt;

        return true;
    }

    public boolean markAsFailed(String failureReason, Instant failedAt) {
        String normalizedFailureReason = normalizeFailureReason(failureReason);
        Instant requiredFailedAt = requireNonNull(failedAt, "Failed at");

        if (hasFailed()) {
            return false;
        }

        if (this.status == InvoiceStatus.GENERATED) {
            throw new IllegalInvoiceStatusChangeException("GENERATED invoice cannot be marked as FAILED");
        }

        this.status = InvoiceStatus.FAILED;
        this.failureReason = normalizedFailureReason;
        this.failedAt = requiredFailedAt;

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Invoice other = (Invoice) obj;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    private static void validateNewInvoice(UUID id, Long orderId) {
        if (id == null) {
            throw new InvalidInvoiceException("Invoice ID must not be null");
        }

        validateOrderId(orderId, InvalidInvoiceException::new);
    }

    private static void validateRehydratedInvoice(UUID id, Long orderId, InvoiceStatus status) {
        if (id == null) {
            throw new InvoiceRehydrationException("Rehydrated invoice must have an ID");
        }

        if (status == null) {
            throw new InvoiceRehydrationException("Rehydrated invoice must have a status");
        }

        validateOrderId(orderId, InvoiceRehydrationException::new);
    }

    private static void validateRehydratedStatusConsistency(
            InvoiceStatus status,
            String storageKey,
            Instant generatedAt,
            Instant failedAt,
            String failureReason
    ) {
        String normalizedStorageKey = normalizeOptionalText(storageKey);
        String normalizedFailureReason = normalizeOptionalText(failureReason);

        switch (status) {
            case PROCESSING -> {
                if (normalizedStorageKey != null || generatedAt != null || failedAt != null || normalizedFailureReason != null) {
                    throw new InvoiceRehydrationException("Rehydrated PROCESSING invoice must not contain generation or failure data");
                }
            }
            case GENERATED -> {
                if (normalizedStorageKey == null || generatedAt == null) {
                    throw new InvoiceRehydrationException("Rehydrated GENERATED invoice must contain storage key and generated date");
                }
                if (failedAt != null || normalizedFailureReason != null) {
                    throw new InvoiceRehydrationException("Rehydrated GENERATED invoice must not contain failure data");
                }
            }
            case FAILED -> {
                if (normalizedFailureReason == null || failedAt == null) {
                    throw new InvoiceRehydrationException("Rehydrated FAILED invoice must contain failure reason and failure date");
                }
                if (normalizedStorageKey != null || generatedAt != null) {
                    throw new InvoiceRehydrationException("Rehydrated FAILED invoice must not contain storage key or generated date");
                }
            }
        }
    }

    private static void validateOrderId(
            Long orderId,
            Function<String, ? extends InvoiceDomainException> exceptionFactory
    ) {
        if (orderId == null) {
            throw exceptionFactory.apply("Order ID must not be null");
        }

        if (orderId <= 0L) {
            throw exceptionFactory.apply("Order ID must be greater than zero");
        }
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static String normalizeFailureReason(String reason) {
        String normalizedReason = requireNonBlank(reason, "Failure reason");

        if (normalizedReason.length() > MAX_FAILURE_REASON_LENGTH) {
            return normalizedReason.substring(0, MAX_FAILURE_REASON_LENGTH);
        }

        return normalizedReason;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidInvoiceException(fieldName + " is required");
        }

        return value.trim();
    }

    private static <T> T requireNonNull(T obj, String fieldName) {
        if (obj == null) {
            throw new InvalidInvoiceException(fieldName + " is required");
        }

        return obj;
    }

    private boolean hasFailed() {
        return this.status == InvoiceStatus.FAILED;
    }

    private void throwIfAlreadyFailed() {
        if (hasFailed()) {
            throw new InvalidInvoiceStateException("Invoice has been failed");
        }
    }
}