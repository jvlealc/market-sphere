package io.github.jvlealc.marketsphere.orders.infrastructure.persistence;

import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.OutboxMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxRepository extends JpaRepository<OutboxMessageJpaEntity, UUID> {

    @Query(
            value = """
                    WITH candidates AS (
                        SELECT id
                        FROM outbox_messages
                        WHERE channel = :channel
                          AND event_type = :eventType
                          AND attempts < max_attempts
                          AND (
                                status IN ('PENDING', 'FAILED')
                                OR (status = 'PROCESSING' AND locked_until <= now())
                          )
                          AND (next_attempt_at IS NULL OR next_attempt_at <= now())
                          AND (locked_until IS NULL OR locked_until <= now())
                        ORDER BY created_at
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED
                    )
                    UPDATE outbox_messages o
                    SET status = 'PROCESSING',
                        locked_until = now() + make_interval(secs => :lockSeconds),
                        updated_at = now()
                    FROM candidates c
                    WHERE o.id = c.id
                    RETURNING o.*
                    """,
            nativeQuery = true
    )
    List<OutboxMessageJpaEntity> claimProcessableMessages(
            @Param("channel") String channel,
            @Param("eventType") String eventType,
            @Param("limit") int limit,
            @Param("lockSeconds") long lockSeconds
    );

    @Modifying
    @Query(
            value = """
                    UPDATE outbox_messages
                    SET status = 'PROCESSED',
                        processed_at = now(),
                        locked_until = NULL,
                        error_message = NULL,
                        updated_at = now()
                    WHERE id = :messageId
                        AND status = 'PROCESSING' 
                    """,
            nativeQuery = true
    )
    int markAsProcessed(@Param("messageId") UUID messageId);

    @Modifying
    @Query(
            value = """
                    UPDATE outbox_messages
                    SET attempts = attempts + 1,
                        status = CASE
                            WHEN attempts + 1 >= max_attempts THEN 'DEAD'
                            ELSE 'FAILED'
                        END,
                        next_attempt_at = CASE
                            WHEN attempts + 1 >= max_attempts THEN NULL
                            ELSE now() + make_interval(secs => :retrySeconds)
                        END,
                        locked_until = NULL,
                        error_message = :errorMessage,
                        updated_at = now()
                    WHERE id = messageId
                        AND status = 'PROCESSING'
                    """,
            nativeQuery = true
    )
    int markAsFailed(
            @Param("messageId") UUID messageId,
            @Param("errorMessage") String errorMessage,
            @Param("retrySeconds") long retrySeconds
    );
}