package io.github.jvlealc.marketsphere.shipping.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxJpaRepository extends JpaRepository<OutboxMessage, UUID> {

    /**
     * Reivindica um lote marcando {@code PROCESSING} no mesmo comando que o seleciona, e grava em
     * {@code next_attempt_at} o prazo a partir do qual a linha volta a ser reivindicável. Uma linha
     * que continua {@code PROCESSING} depois desse prazo teve o worker morto entre publicar e
     * concluir, e é recolhida por esta mesma consulta na rodada seguinte.
     * <p>
     * É o {@code FOR UPDATE SKIP LOCKED} que impede dois workers de pegarem a mesma linha — inclusive
     * com várias instâncias no ar, como durante um rolling deploy.
     */
    @Transactional
    @Query(
            value = """
                    WITH candidates AS (
                        SELECT m.id
                        FROM outbox_messages m
                        WHERE m.attempts < m.max_attempts
                          AND m.status IN ('PENDING', 'FAILED', 'PROCESSING')
                          AND m.next_attempt_at <= now()
                        ORDER BY m.created_at, m.id
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED
                    )
                    UPDATE outbox_messages om
                    SET status = 'PROCESSING',
                        next_attempt_at = now() + make_interval(secs => :claimSeconds),
                        failure_reason = NULL,
                        updated_at = now()
                    FROM candidates c
                    WHERE om.id = c.id
                    RETURNING om.*
                    """,
            nativeQuery = true
    )
    List<OutboxMessage> claimProcessableMessages(
            @Param("limit") int limit,
            @Param("claimSeconds") long claimSeconds
    );

    /**
     * A guarda {@code status = 'PROCESSING'} impede este worker de concluir uma linha cujo prazo
     * expirou e que outro já reivindicou. Zero linhas afetadas é operação normal, não erro.
     */
    @Transactional
    @Modifying
    @Query(
            value = """
                    UPDATE outbox_messages
                    SET status = 'PROCESSED',
                        processed_at = now(),
                        failure_reason = NULL,
                        next_attempt_at = NULL,
                        updated_at = now()
                    WHERE id = :messageId
                      AND status = 'PROCESSING'
                    """,
            nativeQuery = true
    )
    int markAsProcessed(@Param("messageId") UUID messageId);

    /**
     * A promoção a {@code DEAD} ao esgotar as tentativas é decidida no {@code CASE}, e não no worker,
     * para ser atômica com o incremento de {@code attempts}.
     */
    @Transactional
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
                            ELSE :nextAttemptAt
                        END,
                        failure_reason = :failureReason,
                        updated_at = now()
                    WHERE id = :messageId
                      AND status = 'PROCESSING'
                    """,
            nativeQuery = true
    )
    int recordFailure(
            @Param("messageId") UUID messageId,
            @Param("failureReason") String failureReason,
            @Param("nextAttemptAt") Instant nextAttemptAt
    );

    @Transactional
    @Modifying
    @Query(
            value = """
                    UPDATE outbox_messages
                    SET status = 'DEAD',
                        attempts = LEAST(attempts + 1, max_attempts),
                        next_attempt_at = NULL,
                        failure_reason = :failureReason,
                        updated_at = now()
                    WHERE id = :messageId
                      AND status = 'PROCESSING'
                    """,
            nativeQuery = true
    )
    int markAsDead(
            @Param("messageId") UUID messageId,
            @Param("failureReason") String failureReason
    );
}
