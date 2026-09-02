package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.persistence.jpa.outbox;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.github.jvlealc.marketsphere.orders.application.identity.UuidV7;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxAggregateType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxStatus;
import io.github.jvlealc.marketsphere.orders.support.PostgresContainerSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As quatro queries da outbox são SQL nativo, e é por isso que este teste existe: nenhuma suíte unitária
 * alcança um {@code CASE} dentro de {@code UPDATE} nem a semântica de {@code FOR UPDATE SKIP LOCKED}.
 * <p>
 * O {@code ddl-auto=validate} cobre a segunda metade: como o container aplica o {@code schema.sql}
 * versionado na criação, os mapeamentos JPA deste módulo passam a ser conferidos contra o DDL a cada build.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SpringDataOutboxRepositoryIT extends PostgresContainerSupport {

    private static final int EVENT_VERSION = 1;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_SECONDS = 60L;
    private static final long RETRY_SECONDS = 30L;

    private static final OutboxChannel CHANNEL = OutboxChannel.MESSAGING;
    private static final OutboxEventType EVENT_TYPE = OutboxEventType.ORDER_PAID;

    private final SpringDataOutboxRepository repository;
    private final EntityManager entityManager;

    SpringDataOutboxRepositoryIT(SpringDataOutboxRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    // ------------------------------------------------------------------- claim

    @Test
    void shouldClaimOnlyEligibleMessages() {
        Instant now = Instant.now();

        OutboxMessageJpaEntity due = pendingMessage(CHANNEL, EVENT_TYPE, now.minusSeconds(10));
        OutboxMessageJpaEntity alsoDue = pendingMessage(CHANNEL, EVENT_TYPE, now.minusSeconds(5));
        OutboxMessageJpaEntity notYetDue = pendingMessage(CHANNEL, EVENT_TYPE, now.plusSeconds(300));
        OutboxMessageJpaEntity otherChannel = pendingMessage(OutboxChannel.EMAIL, EVENT_TYPE, now.minusSeconds(10));
        OutboxMessageJpaEntity otherEventType =
                pendingMessage(CHANNEL, OutboxEventType.ORDER_READY_FOR_SHIPMENT, now.minusSeconds(10));

        repository.saveAllAndFlush(List.of(due, alsoDue, notYetDue, otherChannel, otherEventType));
        entityManager.clear();

        List<OutboxMessageJpaEntity> claimed = claim(10);

        assertThat(idsOf(claimed)).containsExactlyInAnyOrder(due.getId(), alsoDue.getId());
    }

    /**
     * O ramo de reivindicação de lease expirado. Sem ele, uma mensagem cujo worker morreu no meio da
     * publicação ficaria em {@code PROCESSING} para sempre.
     */
    @Test
    void shouldClaimProcessingMessage_whenLeaseHasExpired() {
        OutboxMessageJpaEntity message = pendingMessage(CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(10));
        repository.saveAndFlush(message);
        entityManager.clear();

        assertThat(claimWithLease(1, -1L)).hasSize(1);
        entityManager.clear();

        assertThat(idsOf(claim(1))).containsExactly(message.getId());
    }

    @Test
    void shouldNotClaimProcessingMessage_whenLeaseIsStillHeld() {
        repository.saveAndFlush(pendingMessage(CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(10)));
        entityManager.clear();

        assertThat(claim(1)).hasSize(1);
        entityManager.clear();

        assertThat(claim(1)).isEmpty();
    }

    @Test
    void shouldNotClaimMessage_whenAttemptsAreExhausted() {
        OutboxMessageJpaEntity exhausted = pendingMessage(
                CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(10), MAX_ATTEMPTS, MAX_ATTEMPTS);

        repository.saveAndFlush(exhausted);
        entityManager.clear();

        assertThat(claim(10)).isEmpty();
    }

    @Test
    void shouldRespectRequestedLimit() {
        repository.saveAllAndFlush(List.of(
                pendingMessage(CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(30)),
                pendingMessage(CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(20)),
                pendingMessage(CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(10))
        ));
        entityManager.clear();

        assertThat(claim(2)).hasSize(2);
    }

    /**
     * O claim precisa deixar a linha coerente com {@code chk_outbox_lock} e
     * {@code chk_outbox_next_attempt_at}: em {@code PROCESSING} o prazo é o do lease, e não há reagendamento
     * pendente.
     */
    @Test
    void shouldTakeOwnershipOfClaimedRow() {
        repository.saveAndFlush(pendingMessage(CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(10)));
        entityManager.clear();

        OutboxMessageJpaEntity claimed = claim(1).getFirst();

        assertThat(claimed.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(claimed.getLockToken()).isNotNull();
        assertThat(claimed.getLockedUntil()).isNotNull();
        assertThat(claimed.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldGiveEachClaimADistinctLockToken() {
        repository.saveAllAndFlush(List.of(
                pendingMessage(CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(20)),
                pendingMessage(CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(10))
        ));
        entityManager.clear();

        Set<UUID> tokens = claim(10).stream()
                .map(OutboxMessageJpaEntity::getLockToken)
                .collect(Collectors.toSet());

        assertThat(tokens).hasSize(2).doesNotContainNull();
    }

    // ----------------------------------------------------------- markAsProcessed

    @Test
    void shouldMarkClaimedMessageAsProcessed() {
        ClaimedRow row = persistAndClaim(0, MAX_ATTEMPTS);

        assertThat(repository.markAsProcessed(row.messageId(), row.lockToken())).isEqualTo(1);

        OutboxMessageJpaEntity stored = reload(row.messageId());
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(stored.getProcessedAt()).isNotNull();
        assertThat(stored.getLockToken()).isNull();
        assertThat(stored.getLockedUntil()).isNull();
        assertThat(stored.getNextAttemptAt()).isNull();
    }

    /**
     * A guarda de token é o que separa "perdi a posse" de "erro": um worker atrasado não pode marcar como
     * entregue a mensagem que outro está publicando neste instante.
     */
    @Test
    void shouldAffectNoRow_whenLockTokenIsStale() {
        ClaimedRow row = persistAndClaim(0, MAX_ATTEMPTS);

        assertThat(repository.markAsProcessed(row.messageId(), UUID.randomUUID())).isZero();
        assertThat(reload(row.messageId()).getStatus()).isEqualTo(OutboxStatus.PROCESSING);
    }

    @Test
    void shouldAffectNoRow_whenMessageIsNotProcessing() {
        OutboxMessageJpaEntity pending = pendingMessage(CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(10));
        repository.saveAndFlush(pending);
        entityManager.clear();

        assertThat(repository.markAsProcessed(pending.getId(), UUID.randomUUID())).isZero();
        assertThat(reload(pending.getId()).getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    // ------------------------------------------------------------- recordFailure

    @Test
    void shouldRecordFailureAndScheduleRetry() {
        ClaimedRow row = persistAndClaim(0, MAX_ATTEMPTS);

        assertThat(repository.recordFailure(row.messageId(), row.lockToken(), "broker unavailable", RETRY_SECONDS))
                .isEqualTo(1);

        OutboxMessageJpaEntity stored = reload(row.messageId());
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(stored.getAttempts()).isEqualTo(1);
        assertThat(stored.getNextAttemptAt()).isNotNull();
        assertThat(stored.getFailureReason()).isEqualTo("broker unavailable");
        assertThat(stored.getLockToken()).isNull();
    }

    /**
     * A promoção a {@code DEAD} acontece dentro do próprio {@code UPDATE}, e zera o prazo junto, uma linha
     * que não será reivindicada de novo não pode carregar reagendamento, e o modelo precisa saber
     * reidratá-la assim.
     */
    @Test
    void shouldPromoteToDead_whenAttemptsAreExhausted() {
        ClaimedRow row = persistAndClaim(MAX_ATTEMPTS - 1, MAX_ATTEMPTS);

        repository.recordFailure(row.messageId(), row.lockToken(), "last attempt", RETRY_SECONDS);

        OutboxMessageJpaEntity stored = reload(row.messageId());
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(stored.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(stored.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldAffectNoRow_whenLockTokenIsStaleOnFailure() {
        ClaimedRow row = persistAndClaim(0, MAX_ATTEMPTS);

        assertThat(repository.recordFailure(row.messageId(), UUID.randomUUID(), "ignored", RETRY_SECONDS))
                .isZero();
        assertThat(reload(row.messageId()).getAttempts()).isZero();
    }

    // ---------------------------------------------------------------- markAsDead

    @Test
    void shouldMarkClaimedMessageAsDead() {
        ClaimedRow row = persistAndClaim(0, MAX_ATTEMPTS);

        assertThat(repository.markAsDead(row.messageId(), row.lockToken(), "payload rejected")).isEqualTo(1);

        OutboxMessageJpaEntity stored = reload(row.messageId());
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(stored.getAttempts()).isEqualTo(1);
        assertThat(stored.getNextAttemptAt()).isNull();
        assertThat(stored.getFailureReason()).isEqualTo("payload rejected");
    }

    /**
     * A falha terminal na última tentativa disponível chega exatamente ao teto, sem violar o
     * {@code chk_outbox_attempts}.
     * <p>
     * O ramo em que o {@code LEAST} de fato corta é inalcançável por este caminho: o claim exige
     * {@code attempts < max_attempts}, então uma linha reivindicada nunca chega aqui no teto.
     */
    @Test
    void shouldNotExceedMaximumAttempts_whenMarkedAsDead() {
        ClaimedRow row = persistAndClaim(MAX_ATTEMPTS - 1, MAX_ATTEMPTS);

        repository.markAsDead(row.messageId(), row.lockToken(), "terminal");

        assertThat(reload(row.messageId()).getAttempts()).isEqualTo(MAX_ATTEMPTS);
    }

    // ------------------------------------------------------------------ helpers

    private List<OutboxMessageJpaEntity> claim(int limit) {
        return claimWithLease(limit, LOCK_SECONDS);
    }

    private List<OutboxMessageJpaEntity> claimWithLease(int limit, long lockSeconds) {
        return repository.claimProcessableMessages(CHANNEL.name(), EVENT_TYPE.name(), limit, lockSeconds);
    }

    private OutboxMessageJpaEntity reload(UUID messageId) {
        entityManager.clear();
        return repository.findById(messageId).orElseThrow();
    }

    private static List<UUID> idsOf(List<OutboxMessageJpaEntity> messages) {
        return messages.stream().map(OutboxMessageJpaEntity::getId).toList();
    }

    /**
     * O claim é o único caminho que concede o {@code lock_token}, então as conclusões só podem ser
     * exercidas a partir de uma linha genuinamente reivindicada.
     */
    private ClaimedRow persistAndClaim(int attempts, int maxAttempts) {
        OutboxMessageJpaEntity pending = pendingMessage(
                CHANNEL, EVENT_TYPE, Instant.now().minusSeconds(10), attempts, maxAttempts);

        repository.saveAndFlush(pending);
        entityManager.clear();

        List<OutboxMessageJpaEntity> claimed = claim(1);

        assertThat(claimed)
                .as("o claim exige attempts < maxAttempts; com %d/%d a linha não é reivindicável",
                        attempts, maxAttempts)
                .hasSize(1);

        OutboxMessageJpaEntity row = claimed.getFirst();
        entityManager.clear();

        return new ClaimedRow(row.getId(), row.getLockToken());
    }

    private static OutboxMessageJpaEntity pendingMessage(
            OutboxChannel channel,
            OutboxEventType eventType,
            Instant nextAttemptAt
    ) {
        return pendingMessage(channel, eventType, nextAttemptAt, 0, MAX_ATTEMPTS);
    }

    private static OutboxMessageJpaEntity pendingMessage(
            OutboxChannel channel,
            OutboxEventType eventType,
            Instant nextAttemptAt,
            int attempts,
            int maxAttempts
    ) {
        return message(channel, eventType, nextAttemptAt, attempts, maxAttempts);
    }

    private static OutboxMessageJpaEntity message(
            OutboxChannel channel,
            OutboxEventType eventType,
            Instant nextAttemptAt,
            int attempts,
            int maxAttempts
    ) {
        UUID messageId = UuidV7.generate();
        long orderId = 100L;

        return new OutboxMessageJpaEntity(
                messageId,
                OutboxAggregateType.ORDER,
                String.valueOf(orderId),
                eventType,
                EVENT_VERSION,
                Instant.now(),
                channel,
                messageKeyFor(channel, orderId),
                UuidV7.generate().toString(),
                null,
                JsonNodeFactory.instance.objectNode().put("orderId", orderId),
                OutboxStatus.PENDING,
                attempts,
                maxAttempts,
                nextAttemptAt,
                "%s-%s-%s".formatted(channel.name().toLowerCase(Locale.ROOT), eventType.name().toLowerCase(Locale.ROOT), messageId),
                null
        );
    }

    /** Espelha {@code chk_outbox_message_key}: só o canal de mensageria tem partição a escolher. */
    private static String messageKeyFor(OutboxChannel channel, long orderId) {
        return channel == OutboxChannel.MESSAGING ? String.valueOf(orderId) : null;
    }

    private record ClaimedRow(UUID messageId, UUID lockToken) {
    }
}
