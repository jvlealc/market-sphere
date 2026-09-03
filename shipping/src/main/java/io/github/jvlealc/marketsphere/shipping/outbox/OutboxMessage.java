package io.github.jvlealc.marketsphere.shipping.outbox;

import io.github.jvlealc.marketsphere.shipping.identity.UuidV7;
import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

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
        EventLineage requiredLineage = requireNonNull(lineage, "lineage");

        this.id = UuidV7.generate();
        this.aggregateId = requireNonBlank(aggregateId, "aggregateId");
        this.eventType = requireNonNull(eventType, "eventType");
        this.eventVersion = requirePositiveVersion(eventVersion);
        this.occurredAt = requireNonNull(occurredAt, "occurredAt");
        this.messageKey = requireNonBlank(messageKey, "messageKey");
        this.correlationId = requireNonBlank(requiredLineage.correlationId(), "correlationId");
        this.causationId = normalizeStr(requiredLineage.causationId());
        this.payload = requireNonNull(payload, "payload").value();
        this.idempotencyKey = requireNonBlank(idempotencyKey, "idempotencyKey");
        this.nextAttemptAt = requireNonNull(nextAttemptAt, "nextAttemptAt");

        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.maxAttempts = DEFAULT_MAX_ATTEMPTS;
        this.processedAt = null;
        this.failureReason = null;
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
                aggregateId,
                eventType,
                eventVersion,
                occurredAt,
                messageKey,
                lineage,
                payload,
                nextAttemptAt,
                idempotencyKey
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

    private static String normalizeStr(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }

        return value.trim();
    }

    private static <T>T requireNonNull(T obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }

        return obj;
    }

    private static int requirePositiveVersion(int eventVersion) {
        if (eventVersion <= 0) {
            throw new IllegalArgumentException("eventVersion must be greater than zero");
        }

        return eventVersion;
    }
}