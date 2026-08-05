package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.persistence.jpa.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxAggregateType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "outbox_messages")
@EqualsAndHashCode(of = "id")
class OutboxMessageJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private OutboxAggregateType aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private OutboxEventType eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OutboxChannel channel;

    @Column(name = "message_key", length = 200)
    private String messageKey;

    /**
     * {@code varchar}, e não {@code uuid}: são identificadores de origem externa, e o formato deles não é
     * nosso para fixar. Ver o comentário da coluna no {@code schema.sql}.
     */
    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "causation_id", length = 64)
    private String causationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

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

    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * Preenchido pelo {@code UPDATE} de reivindicação e lido de volta pelo {@code RETURNING}. Nunca é
     * escrito por esta entidade: o lease é concedido e revogado exclusivamente por SQL.
     */
    @Column(name = "lock_token")
    private UUID lockToken;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failure_reason", length = 2_000)
    private String failureReason;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean newEntity = true;

    OutboxMessageJpaEntity(
            UUID id,
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            OutboxChannel channel,
            String messageKey,
            String correlationId,
            String causationId,
            JsonNode payload,
            OutboxStatus status,
            int attempts,
            int maxAttempts,
            Instant nextAttemptAt,
            String idempotencyKey,
            String failureReason
    ) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.occurredAt = occurredAt;
        this.channel = channel;
        this.messageKey = messageKey;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = nextAttemptAt;
        this.idempotencyKey = idempotencyKey;
        this.failureReason = failureReason;
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
}
