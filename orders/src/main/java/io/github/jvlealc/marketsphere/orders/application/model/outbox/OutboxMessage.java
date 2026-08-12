package io.github.jvlealc.marketsphere.orders.application.model.outbox;

import io.github.jvlealc.marketsphere.orders.application.identity.UuidV7;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class OutboxMessage {

    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final UUID id;
    private final OutboxAggregateType aggregateType;
    private final String aggregateId;
    private final OutboxEventType eventType;
    private final int eventVersion;
    private final Instant occurredAt;
    private final OutboxChannel channel;
    private final String messageKey;
    private final SerializedOutboxPayload payload;
    private final OutboxStatus status;
    private final int attempts;
    private final int maxAttempts;
    private final Instant nextAttemptAt;
    private final String idempotencyKey;
    private final EventLineage eventLineage;
    private final OutboxFailureReason failureReason;

    private OutboxMessage(
            UUID id,
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            OutboxChannel channel,
            String messageKey,
            SerializedOutboxPayload payload,
            OutboxStatus status,
            int attempts,
            int maxAttempts,
            Instant nextAttemptAt,
            String idempotencyKey,
            EventLineage eventLineage,
            OutboxFailureReason failureReason
    ) {
        validateAttemptsConsistency(attempts, maxAttempts);

        this.id = requireNonNull(id, "Outbox message ID must not be null");
        this.aggregateType = requireNonNull(aggregateType, "Aggregate type must not be null");
        this.aggregateId = requireText(aggregateId, "Aggregate ID is required");
        this.eventType = requireNonNull(eventType, "Event type must not be null");
        this.eventVersion = requirePositiveVersion(eventVersion);
        this.occurredAt = requireNonNull(occurredAt, "Occurrence date must not be null");
        this.channel = requireNonNull(channel, "Channel must not be null");
        this.messageKey = requireValidMessageKey(this.channel, messageKey);
        this.payload = requireNonNull(payload, "Payload must not be null");
        this.status = requireNonNull(status, "Outbox status must not be null");
        this.nextAttemptAt = requireValidNextAttemptAt(this.status, nextAttemptAt);
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.idempotencyKey = requireText(idempotencyKey, "Idempotency key is required");
        this.eventLineage = requireNonNull(eventLineage, "Event lineage must not be null");
        this.failureReason = failureReason;
    }

    public static OutboxMessage createNew(
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            OutboxChannel channel,
            String messageKey,
            SerializedOutboxPayload payload,
            String idempotencyKey,
            EventLineage eventLineage,
            Instant nextAttemptAt
    ) {
        return new OutboxMessage(
                UuidV7.generate(),
                aggregateType,
                aggregateId,
                eventType,
                eventVersion,
                occurredAt,
                channel,
                messageKey,
                payload,
                OutboxStatus.PENDING,
                0,
                DEFAULT_MAX_ATTEMPTS,
                nextAttemptAt,
                idempotencyKey,
                eventLineage,
                null
        );
    }

    public static OutboxMessage rehydrate(
            UUID id,
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            OutboxChannel channel,
            String messageKey,
            SerializedOutboxPayload payload,
            OutboxStatus status,
            int attempts,
            int maxAttempts,
            Instant nextAttemptAt,
            String idempotencyKey,
            EventLineage eventLineage,
            OutboxFailureReason failureReason
    ) {
        return new OutboxMessage(
                id,
                aggregateType,
                aggregateId,
                eventType,
                eventVersion,
                occurredAt,
                channel,
                messageKey,
                payload,
                status,
                attempts,
                maxAttempts,
                nextAttemptAt,
                idempotencyKey,
                eventLineage,
                failureReason
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

    public int getEventVersion() {
        return eventVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public OutboxChannel getChannel() {
        return channel;
    }

    /**
     * Chave de particionamento do Kafka. Nula nos canais de EMAIL e PAYMENT.
     */
    public String getMessageKey() {
        return messageKey;
    }

    public SerializedOutboxPayload getPayload() {
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

    public EventLineage getEventLineage() {
        return eventLineage;
    }

    public OutboxFailureReason getFailureReason() {
        return failureReason;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static int requirePositiveVersion(int eventVersion) {
        if (eventVersion <= 0) {
            throw new IllegalArgumentException("Event version must be greater than zero");
        }

        return eventVersion;
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

    private static String requireValidMessageKey(OutboxChannel channel, String messageKey) {
        return switch (channel) {
            case MESSAGING -> requireText(messageKey, "Message key is required for channel " + channel);

            case EMAIL, PAYMENT -> {
                if (messageKey != null) {
                    throw new IllegalArgumentException("Message key must be null for channel " + channel);
                }

                yield null;
            }
        };
    }

    private static Instant requireValidNextAttemptAt(OutboxStatus status, Instant nextAttemptAt) {
        return switch (status) {
            case PENDING, FAILED -> requireNonNull(
                    nextAttemptAt,
                    "Next attempt date must not be null for status " + status + " outbox message"
            );

            case PROCESSING, PROCESSED, DEAD ->  {
                if (nextAttemptAt != null) {
                    throw new IllegalArgumentException("Next attempt date must be null for status " + status + " outbox message");
                }

                yield null;
            }
        };
    }
}