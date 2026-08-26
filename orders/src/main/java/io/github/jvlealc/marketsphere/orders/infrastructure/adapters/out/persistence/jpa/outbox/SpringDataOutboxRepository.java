package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.persistence.jpa.outbox;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataOutboxRepository extends JpaRepository<OutboxMessageJpaEntity, UUID> {

    /**
     * CTE com {@code FOR UPDATE SKIP LOCKED} seguida de {@code UPDATE ... RETURNING}: a forma correta de
     * fazer claim concorrente em Postgres, cada worker leva um lote disjunto sem bloquear os demais.
     * <p>
     * O {@code lock_token} novo é gerado aqui, no mesmo {@code UPDATE} que concede o lease, e é o que as
     * conclusões exigem depois.
     */
    @Query(
            value = """
                    WITH candidates AS (
                        SELECT m.id
                        FROM outbox_messages m
                        WHERE m.channel = :channel
                          AND m.event_type = :eventType
                          AND m.attempts < m.max_attempts
                          AND (
                              (
                                  m.status IN ('PENDING', 'FAILED')
                                  AND m.next_attempt_at <= now()
                              )
                              OR
                              (
                                  m.status = 'PROCESSING'
                                  AND m.locked_until <= now()
                              )
                          )
                        ORDER BY m.created_at, m.id
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED
                    )
                    UPDATE outbox_messages om
                    SET status = 'PROCESSING',
                        next_attempt_at = NULL,
                        locked_until = now() + make_interval(secs => :lockSeconds),
                        lock_token = gen_random_uuid(),
                        failure_reason = NULL,
                        updated_at = now()
                    FROM candidates c
                    WHERE om.id = c.id
                    RETURNING om.*
                    """,
            nativeQuery = true
    )
    List<OutboxMessageJpaEntity> claimProcessableMessages(
            @Param("channel") String channel,
            @Param("eventType") String eventType,
            @Param("limit") int limit,
            @Param("lockSeconds") long lockSeconds
    );

    /**
     * O {@code lock_token} na cláusula {@code WHERE} é o que impede um worker cujo lease expirou de
     * concluir a mensagem que outro está publicando neste instante. Zero linhas afetadas significa
     * "perdeu o lease", não "erro".
     */
    @Modifying
    @Query(
            value = """
                    UPDATE outbox_messages
                    SET status = 'PROCESSED',
                        processed_at = now(),
                        locked_until = NULL,
                        lock_token = NULL,
                        failure_reason = NULL,
                        next_attempt_at = NULL,
                        updated_at = now()
                    WHERE id = :messageId
                        AND status = 'PROCESSING'
                        AND lock_token = :lockToken
                    """,
            nativeQuery = true
    )
    int markAsProcessed(
            @Param("messageId") UUID messageId,
            @Param("lockToken") UUID lockToken
    );

    /**
     * A promoção a {@code DEAD} ao esgotar as tentativas é decidida aqui, no {@code CASE}, e não no worker:
     * uma regra, um lugar.
     */
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
                        lock_token = NULL,
                        failure_reason = :failureReason,
                        updated_at = now()
                    WHERE id = :messageId
                        AND status = 'PROCESSING'
                        AND lock_token = :lockToken
                    """,
            nativeQuery = true
    )
    int recordFailure(
            @Param("messageId") UUID messageId,
            @Param("lockToken") UUID lockToken,
            @Param("failureReason") String failureReason,
            @Param("retrySeconds") long retrySeconds
    );

    @Modifying
    @Query(
            value = """
                    UPDATE outbox_messages
                    SET status = 'DEAD',
                        attempts = LEAST(attempts + 1, max_attempts),
                        locked_until = NULL,
                        lock_token = NULL,
                        next_attempt_at = NULL,
                        failure_reason = :failureReason,
                        updated_at = now()
                    WHERE id = :messageId
                        AND status = 'PROCESSING'
                        AND lock_token = :lockToken
                    """,
            nativeQuery = true
    )
    int markAsDead(
            @Param("messageId") UUID messageId,
            @Param("lockToken") UUID lockToken,
            @Param("failureReason") String failureReason
    );
}