package io.github.jvlealc.marketsphere.orders.application.ports.out.outbox;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class OutboxMessage {

    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final long MAX_ERROR_MESSAGE_LENGTH = 2_000L;

    private final UUID id;
    private final OutboxAggregateType aggregateType;
    private final String aggregateId;
    private final OutboxEventType eventType;
    private final OutboxChannel channel;
    private final String payload;
    private final OutboxStatus status;
    private final int attempts;
    private final int maxAttempts;
    private final Instant nextAttemptAt;
    private final String idempotencyKey;
    private final String errorMessage;

    private OutboxMessage(
            UUID id,
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            OutboxChannel channel,
            String payload,
            OutboxStatus status,
            int attempts,
            int maxAttempts,
            Instant nextAttemptAt,
            String idempotencyKey,
            String errorMessage
    ) {
        validateAttemptsConsistency(attempts, maxAttempts);

        this.id = requireNonNull(id, "Outbox message ID must not be null");
        this.aggregateType = requireNonNull(aggregateType, "Aggregate type must not be null");
        this.aggregateId = requireText(aggregateId, "Aggregate ID is required");
        this.eventType = requireNonNull(eventType, "Event type must not be null");
        this.channel = requireNonNull(channel, "Channel must not be null");
        this.payload = requireText(payload, "Payload is required");
        this.status = requireNonNull(status, "Outbox status must not be null");
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = requireNonNull(nextAttemptAt, "Next attempt date must not be null");
        this.idempotencyKey = requireText(idempotencyKey, "Idempotency key is required");
        this.errorMessage = normalizeErrorMessage(errorMessage);
    }

    public static OutboxMessage createNew(
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            OutboxChannel channel,
            String payload,
            String idempotencyKey
    ) {
        return new OutboxMessage(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                channel,
                payload,
                OutboxStatus.PENDING,
                0,
                DEFAULT_MAX_ATTEMPTS,
                Instant.now(),
                idempotencyKey,
                null
        );
    }

    public static OutboxMessage rehydrate(
            UUID id,
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            OutboxChannel channel,
            String payload,
            OutboxStatus status,
            int attempts,
            int maxAttempts,
            Instant nextAttemptAt,
            String idempotencyKey,
            String errorMessage
    ) {
        validateAttemptsConsistency(attempts, maxAttempts);
        return new OutboxMessage(
                id,
                aggregateType,
                aggregateId,
                eventType,
                channel,
                payload,
                status,
                attempts,
                maxAttempts,
                nextAttemptAt,
                idempotencyKey,
                errorMessage
        );
    }

    public UUID getId() {
        return id;
    }

    public OutboxAggregateType getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public OutboxEventType getEventType() {
        return eventType;
    }

    public OutboxChannel getChannel() {
        return channel;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static void validateAttemptsConsistency(int attempts, int maxAttempts) {
        if (attempts < 0) {
            throw new IllegalArgumentException("Attempts must not be negative");
        }

        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max attempts must be greater than zero");
        }

        if (attempts > maxAttempts) {
            throw new IllegalArgumentException("Attempts must not be greater than max attempts");
        }
    }

    private static String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }

        return errorMessage.trim();
    }
}