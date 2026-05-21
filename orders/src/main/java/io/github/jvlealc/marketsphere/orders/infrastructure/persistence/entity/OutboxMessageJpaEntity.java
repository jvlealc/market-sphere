package io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity;

import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxAggregateType;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "outbox_messages")
@EqualsAndHashCode(of = "id")
@ToString
public class OutboxMessageJpaEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OutboxChannel channel;

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

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}