package io.github.jvlealc.marketsphere.shipping.outbox;


import io.github.jvlealc.marketsphere.shipping.identity.UuidV7;
import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "outbox_messages")
class OutboxMessage implements Persistable<UUID> {

    private static final int DEFAULT_MAX_ATTEMPTS = 10;

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private OutboxEventType eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "message_key", length = 200)
    private String messageKey;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "causation_id", length = 64)
    private String causationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failure_reason", length = 2_000)
    private OutboxFailureReason failureReason;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean newEntity = true;

    protected OutboxMessage() {
    }

    private OutboxMessage(
            UUID id,
            String aggregateId,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            String messageKey,
            String correlationId,
            String causationId,
            SerializedOutboxPayload payload,
            OutboxStatus status,
            int attempts,
            int maxAttempts,
            Instant nextAttemptAt,
            String idempotencyKey,
            Instant processedAt,
            OutboxFailureReason failureReason
    ) {
        validateAttemptsConsistency(attempts, maxAttempts);

        this.id = requireNonNull(id, "Outbox message ID must not be null");
        this.aggregateId = requireText(aggregateId, "Aggregate ID");
        this.eventType = requireNonNull(eventType, "Event type must not be null");
        this.eventVersion = requirePositiveVersion(eventVersion);
        this.occurredAt = requireNonNull(occurredAt, "Occurrence date must not be null");
        this.messageKey = requireText(messageKey, "Message key");
        this.correlationId = requireText(correlationId, "Correlation ID");
        this.causationId = optionalText(causationId);
        this.payload = requireNonNull(payload, "Payload must not be null").value();
        this.status = requireNonNull(status, "Outbox status must not be null");
        this.nextAttemptAt = requireValidNextAttemptAt(this.status, nextAttemptAt);
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.idempotencyKey = requireText(idempotencyKey, "Idempotency key");
        this.processedAt = requireValidProcessedAt(this.status, processedAt);
        this.failureReason = failureReason;
    }

    public static OutboxMessage createNew(
            String aggregateId,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            String messageKey,
            EventLineage lineage,
            SerializedOutboxPayload payload,
            Instant nextAttemptAt,
            String idempotencyKey
    ) {
        return new OutboxMessage(
                UuidV7.generate(),
                aggregateId,
                eventType,
                eventVersion,
                occurredAt,
                messageKey,
                lineage.correlationId(),
                lineage.causationId(),
                payload,
                OutboxStatus.PENDING,
                0,
                DEFAULT_MAX_ATTEMPTS,
                nextAttemptAt,
                idempotencyKey,
                null,
                null
        );
    }

    @Override
    public UUID getId() {
        return id;
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

    public String getMessageKey() {
        return messageKey;
    }

    public EventLineage getEventLineage() {
        return new EventLineage(this.correlationId, this.causationId);
    }

    public SerializedOutboxPayload getPayload() {
        return new SerializedOutboxPayload(payload);
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

    public Instant getProcessedAt() {
        return processedAt;
    }

    public OutboxFailureReason getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    protected void markAsNotNew() {
        this.newEntity = false;
    }

    /**
     * {@code causationId} nulo é estado normal: identifica a raiz de um fluxo, não uma ausência de dado.
     */
    private static String optionalText(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName +  " is required");
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
    private static Instant requireValidNextAttemptAt(OutboxStatus status, Instant nextAttemptAt) {
        return switch (status) {
            case PENDING, FAILED, PROCESSING -> requireNonNull(
                    nextAttemptAt,
                    "Next attempt date must not be null for status " + status + " outbox message"
            );

            case PROCESSED, DEAD ->  {
                if (nextAttemptAt != null) {
                    throw new IllegalArgumentException("Next attempt date must be null for status " + status + " outbox message");
                }

                yield null;
            }
        };
    }

    private static Instant requireValidProcessedAt(OutboxStatus status, Instant processedAt) {
        return switch (status) {
            case PROCESSED -> requireNonNull(
                    processedAt,
                    "Processed at date must be null for status " + status + " outbox message"
            );

            case PENDING, PROCESSING, FAILED, DEAD -> {
                if (processedAt != null) {
                    throw new IllegalArgumentException("Next processed at date must be null for status " + status + " outbox message");
                }
                yield null;
            }
        };
    }
}