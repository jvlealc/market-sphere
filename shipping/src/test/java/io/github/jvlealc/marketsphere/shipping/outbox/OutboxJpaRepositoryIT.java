package io.github.jvlealc.marketsphere.shipping.outbox;

import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import io.github.jvlealc.marketsphere.shipping.support.PostgresContainerSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As quatro consultas da outbox são SQL nativo, e é por isso que este teste existe: nenhuma suíte
 * unitária alcança o {@code CASE} dentro do {@code UPDATE}, a guarda {@code status = 'PROCESSING'} nem a
 * semântica de {@code FOR UPDATE SKIP LOCKED}.
 * <p>
 * O {@code ddl-auto=validate} cobre a segunda metade: como o container aplica o {@code schema.sql}
 * versionado na criação, os mapeamentos JPA do módulo passam a ser conferidos contra o DDL a cada build.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OutboxJpaRepositoryIT extends PostgresContainerSupport {

    private static final int MAX_ATTEMPTS = 10;
    private static final int EVENT_VERSION = 1;
    private static final long CLAIM_SECONDS = 30L;

    private static final String AGGREGATE_ID = "018f4b0e-6f2a-7c31-9a44-5c1d2e3f4a5b";
    private static final String MESSAGE_KEY = "100";
    private static final String CORRELATION_ID = "01a05a27-32af-7c42-b1fa-2a239481cb3e";
    private static final String FAILURE_REASON = "OutboxDeliveryException: broker is unreachable";

    private static final Instant OCCURRED_AT = Instant.parse("2026-09-01T10:00:00Z");
    private static final Instant RETRY_AT = Instant.parse("2026-09-01T10:05:00Z");

    private static final OutboxEventType EVENT_TYPE = OutboxEventType.ORDER_SHIPPED;
    private static final SerializedOutboxPayload PAYLOAD =
            new SerializedOutboxPayload("{\"orderId\":100,\"trackingCode\":\"BR-2ijs7Su29DaA5\"}");

    private final OutboxJpaRepository repository;
    private final EntityManager entityManager;

    OutboxJpaRepositoryIT(OutboxJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    // ------------------------------------------------------------ reivindicação

    @Test
    void shouldClaimOnlyMessagesThatAreDue() {
        OutboxMessage due = persist(Instant.now().minusSeconds(10));
        persist(Instant.now().plusSeconds(300));

        assertThat(idsOf(claim(10))).containsExactly(due.getId());
    }

    /**
     * O ramo de retomada de prazo expirado. Sem ele, uma mensagem cujo worker morreu entre publicar e
     * concluir ficaria em {@code PROCESSING} para sempre.
     */
    @Test
    void shouldClaimProcessingMessage_whenClaimDeadlineHasPassed() {
        OutboxMessage message = persist(Instant.now().minusSeconds(10));

        assertThat(claim(1, -1L)).hasSize(1);

        assertThat(idsOf(claim(1))).containsExactly(message.getId());
    }

    @Test
    void shouldNotClaimProcessingMessage_whenClaimIsStillHeld() {
        persist(Instant.now().minusSeconds(10));

        assertThat(claim(1)).hasSize(1);

        assertThat(claim(1)).isEmpty();
    }

    @Test
    void shouldNotClaimMessage_whenAttemptsRanOut() {
        OutboxMessage message = persist(Instant.now().minusSeconds(10));
        setAttempts(message.getId(), MAX_ATTEMPTS);

        assertThat(claim(10)).isEmpty();
    }

    @Test
    void shouldRespectRequestedLimit() {
        persist(Instant.now().minusSeconds(30));
        persist(Instant.now().minusSeconds(20));
        persist(Instant.now().minusSeconds(10));

        assertThat(claim(2)).hasSize(2);
    }

    /**
     * A linha reivindicada precisa ficar coerente com {@code chk_outbox_next_attempt_at}: em
     * {@code PROCESSING} o prazo existe e é o da reivindicação.
     */
    @Test
    void shouldTakeOwnershipOfClaimedRow() {
        persist(Instant.now().minusSeconds(10));

        OutboxMessage claimed = claim(1).getFirst();

        assertThat(claimed.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(claimed.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(claimed.getFailureReason()).isNull();
    }

    // --------------------------------------------------------- markAsProcessed

    @Test
    void shouldMarkClaimedMessageAsProcessed() {
        UUID messageId = persistAndClaim();

        assertThat(repository.markAsProcessed(messageId)).isEqualTo(1);

        OutboxMessage stored = reload(messageId);
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(stored.getProcessedAt()).isNotNull();
        assertThat(stored.getNextAttemptAt()).isNull();
        assertThat(stored.getFailureReason()).isNull();
    }

    /**
     * A guarda de estado é o que separa "perdi a reivindicação" de "erro": um worker atrasado não pode
     * marcar como entregue a mensagem que outro está publicando neste instante.
     */
    @Test
    void shouldNotMarkAsProcessed_whenMessageIsNotProcessing() {
        OutboxMessage pending = persist(Instant.now().minusSeconds(10));

        assertThat(repository.markAsProcessed(pending.getId())).isZero();
    }

    // ------------------------------------------------------------ recordFailure

    @Test
    void shouldRecordFailureAndScheduleRetry() {
        UUID messageId = persistAndClaim();

        assertThat(repository.recordFailure(messageId, FAILURE_REASON, RETRY_AT)).isEqualTo(1);

        OutboxMessage stored = reload(messageId);
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(stored.getAttempts()).isEqualTo(1);
        assertThat(stored.getNextAttemptAt()).isEqualTo(RETRY_AT);
        assertThat(stored.getFailureReason().value()).isEqualTo(FAILURE_REASON);
    }

    /**
     * A promoção a {@code DEAD} é decidida no {@code CASE}, e não no worker, para ser atômica com o
     * incremento de {@code attempts}.
     */
    @Test
    void shouldPromoteToDead_whenAttemptsRunOut() {
        OutboxMessage message = persist(Instant.now().minusSeconds(10));
        setAttempts(message.getId(), MAX_ATTEMPTS - 1);
        claim(1);

        assertThat(repository.recordFailure(message.getId(), FAILURE_REASON, RETRY_AT)).isEqualTo(1);

        OutboxMessage stored = reload(message.getId());
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(stored.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(stored.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldNotRecordFailure_whenMessageIsNotProcessing() {
        OutboxMessage pending = persist(Instant.now().minusSeconds(10));

        assertThat(repository.recordFailure(pending.getId(), FAILURE_REASON, RETRY_AT)).isZero();
    }

    // -------------------------------------------------------------- markAsDead

    @Test
    void shouldMarkClaimedMessageAsDead() {
        UUID messageId = persistAndClaim();

        assertThat(repository.markAsDead(messageId, FAILURE_REASON)).isEqualTo(1);

        OutboxMessage stored = reload(messageId);
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(stored.getAttempts()).isEqualTo(1);
        assertThat(stored.getNextAttemptAt()).isNull();
        assertThat(stored.getFailureReason().value()).isEqualTo(FAILURE_REASON);
    }

    @Test
    void shouldNotExceedMaximumAttempts_whenMarkedAsDead() {
        UUID messageId = persistAndClaim();
        setAttempts(messageId, MAX_ATTEMPTS);

        assertThat(repository.markAsDead(messageId, FAILURE_REASON)).isEqualTo(1);

        assertThat(reload(messageId).getAttempts()).isEqualTo(MAX_ATTEMPTS);
    }

    @Test
    void shouldNotMarkAsDead_whenMessageIsNotProcessing() {
        OutboxMessage pending = persist(Instant.now().minusSeconds(10));

        assertThat(repository.markAsDead(pending.getId(), FAILURE_REASON)).isZero();
    }

    // ---------------------------------------------------------------- fixtures

    private OutboxMessage persist(Instant nextAttemptAt) {
        OutboxMessage saved = repository.saveAndFlush(OutboxMessage.createNew(
                AGGREGATE_ID,
                EVENT_TYPE,
                EVENT_VERSION,
                OCCURRED_AT,
                MESSAGE_KEY,
                new EventLineage(CORRELATION_ID, null),
                PAYLOAD,
                nextAttemptAt,
                "order-shipped-shipment-" + UUID.randomUUID()
        ));

        entityManager.clear();

        return saved;
    }

    private UUID persistAndClaim() {
        persist(Instant.now().minusSeconds(10));

        return claim(1).getFirst().getId();
    }

    private List<OutboxMessage> claim(int limit) {
        return claim(limit, CLAIM_SECONDS);
    }

    /**
     * O {@code clear} é obrigatório: o {@code RETURNING} devolve linhas que o Hibernate hidrata no
     * contexto de persistência, e uma instância já carregada venceria os valores recém-atualizados.
     */
    private List<OutboxMessage> claim(int limit, long claimSeconds) {
        List<OutboxMessage> claimed = repository.claimProcessableMessages(limit, claimSeconds);

        entityManager.clear();

        return claimed;
    }

    private OutboxMessage reload(UUID messageId) {
        entityManager.clear();

        return repository.findById(messageId).orElseThrow();
    }

    private void setAttempts(UUID messageId, int attempts) {
        entityManager.createNativeQuery("update outbox_messages set attempts = :attempts where id = :id")
                .setParameter("attempts", attempts)
                .setParameter("id", messageId)
                .executeUpdate();

        entityManager.clear();
    }

    private static List<UUID> idsOf(List<OutboxMessage> messages) {
        return messages.stream().map(OutboxMessage::getId).toList();
    }
}
