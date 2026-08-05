package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.persistence.jpa.outbox;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.github.jvlealc.marketsphere.billing.application.identity.UuidV7;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxAggregateType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/schema.sql",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SpringDataOutboxRepositoryIT {

    private static final int EVENT_VERSION = 1;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:17.6");

    private final SpringDataOutboxRepository repository;
    private final EntityManager entityManager;

    SpringDataOutboxRepositoryIT(SpringDataOutboxRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void shouldClaimOnlyEligibleMessagesAndReturnUpdatedEntities() {
        Instant now = Instant.now();

        OutboxMessageJpaEntity firstEligibleEmail = pendingMessage(OutboxChannel.EMAIL, now.minusSeconds(10));
        OutboxMessageJpaEntity secondEligibleEmail = pendingMessage(OutboxChannel.EMAIL, now.minusSeconds(5));
        OutboxMessageJpaEntity messagingMessage = pendingMessage(OutboxChannel.MESSAGING, now.minusSeconds(10));
        OutboxMessageJpaEntity futureEmailMessage = pendingMessage(OutboxChannel.EMAIL, now.plusSeconds(300));

        repository.saveAllAndFlush(
                List.of(
                        firstEligibleEmail,
                        secondEligibleEmail,
                        messagingMessage,
                        futureEmailMessage
                )
        );

        entityManager.clear();

        List<OutboxMessageJpaEntity> claimed =
                repository.claimProcessableMessages(
                        OutboxChannel.EMAIL.name(),
                        OutboxEventType.ORDER_BILLED.name(),
                        2,
                        60L
                );

        assertThat(claimed)
                .hasSize(2)
                .allSatisfy(message -> {
                    assertThat(message.getChannel())
                            .isEqualTo(OutboxChannel.EMAIL);

                    assertThat(message.getEventType())
                            .isEqualTo(
                                    OutboxEventType.ORDER_BILLED
                            );

                    assertThat(message.getStatus())
                            .isEqualTo(OutboxStatus.PROCESSING);

                    assertThat(message.getNextAttemptAt())
                            .isNull();

                    assertThat(message.getLockedUntil())
                            .isNotNull()
                            .isAfter(now);

                    // O lease só existe se vier acompanhado do token que prova a posse.
                    assertThat(message.getLockToken())
                            .isNotNull();

                    assertThat(message.getFailureReason())
                            .isNull();
                });

        Set<UUID> claimedIds = claimed.stream()
                .map(OutboxMessageJpaEntity::getId)
                .collect(Collectors.toSet());

        assertThat(claimedIds)
                .containsExactlyInAnyOrder(
                        firstEligibleEmail.getId(),
                        secondEligibleEmail.getId()
                )
                .doesNotContain(
                        messagingMessage.getId(),
                        futureEmailMessage.getId()
                );

        entityManager.clear();

        List<OutboxMessageJpaEntity> persistedClaimedMessages =
                repository.findAllById(claimedIds);

        assertThat(persistedClaimedMessages)
                .hasSize(2)
                .allSatisfy(message -> {
                    assertThat(message.getStatus())
                            .isEqualTo(OutboxStatus.PROCESSING);

                    assertThat(message.getNextAttemptAt())
                            .isNull();

                    assertThat(message.getLockedUntil())
                            .isNotNull();
                });

        List<OutboxMessageJpaEntity> secondClaim =
                repository.claimProcessableMessages(
                        OutboxChannel.EMAIL.name(),
                        OutboxEventType.ORDER_BILLED.name(),
                        10,
                        60L
                );

        assertThat(secondClaim).isEmpty();
    }

    @Test
    void shouldGiveEachClaimADistinctLockToken() {
        repository.saveAllAndFlush(
                List.of(
                        pendingMessage(OutboxChannel.EMAIL, Instant.now().minusSeconds(10)),
                        pendingMessage(OutboxChannel.EMAIL, Instant.now().minusSeconds(10))
                )
        );

        entityManager.clear();

        List<OutboxMessageJpaEntity> claimed =
                repository.claimProcessableMessages(
                        OutboxChannel.EMAIL.name(),
                        OutboxEventType.ORDER_BILLED.name(),
                        2,
                        60L
                );

        Set<UUID> lockTokens = claimed.stream()
                .map(OutboxMessageJpaEntity::getLockToken)
                .collect(Collectors.toSet());

        assertThat(lockTokens).hasSize(2);
    }

    @Test
    void shouldMarkProcessingMessageAsProcessed() {
        ClaimedRow claimedRow = persistAndClaimMessage(0, 5);

        int updatedRows = repository.markAsProcessed(claimedRow.messageId(), claimedRow.lockToken());

        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();

        OutboxMessageJpaEntity persisted = repository.findById(claimedRow.messageId())
                .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.PROCESSED);

        assertThat(persisted.getProcessedAt()).isNotNull();
        assertThat(persisted.getLockedUntil()).isNull();
        assertThat(persisted.getLockToken()).isNull();
        assertThat(persisted.getNextAttemptAt()).isNull();
        assertThat(persisted.getFailureReason()).isNull();
        assertThat(persisted.getAttempts()).isZero();
    }

    /**
     * A janela que o {@code lock_token} fecha: o worker A travou, o lease expirou, o worker B reivindicou a
     * mesma linha e <em>ainda está publicando</em>. O status continua {@code PROCESSING}, então a guarda de
     * status sozinha deixaria A concluir a mensagem de B.
     */
    @Test
    void shouldNotLetAWorkerWithAStaleLockTokenCompleteTheMessage() {
        ClaimedRow claimedRow = persistAndClaimMessage(0, 5);

        UUID staleLockToken = UUID.randomUUID();

        assertThat(repository.markAsProcessed(claimedRow.messageId(), staleLockToken))
                .isZero();

        assertThat(repository.recordFailure(claimedRow.messageId(), staleLockToken, "Stale worker", 30L))
                .isZero();

        assertThat(repository.markAsDead(claimedRow.messageId(), staleLockToken, "Stale worker"))
                .isZero();

        entityManager.clear();

        OutboxMessageJpaEntity persisted = repository.findById(claimedRow.messageId())
                .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.PROCESSING);

        assertThat(persisted.getLockToken())
                .isEqualTo(claimedRow.lockToken());

        assertThat(persisted.getAttempts()).isZero();
    }

    @Test
    void shouldRecordFailureAndScheduleRetry() {
        ClaimedRow claimedRow = persistAndClaimMessage(0, 5);

        Instant beforeFailure = Instant.now();

        int updatedRows = repository.recordFailure(
                claimedRow.messageId(),
                claimedRow.lockToken(),
                "Temporary messaging failure",
                30L
        );

        Instant afterFailure = Instant.now();

        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();

        OutboxMessageJpaEntity persisted = repository.findById(claimedRow.messageId())
                .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.FAILED);

        assertThat(persisted.getAttempts()).isEqualTo(1);
        assertThat(persisted.getLockedUntil()).isNull();
        assertThat(persisted.getLockToken()).isNull();
        assertThat(persisted.getProcessedAt()).isNull();

        assertThat(persisted.getFailureReason())
                .isEqualTo("Temporary messaging failure");

        assertThat(persisted.getNextAttemptAt())
                .isNotNull()
                .isAfter(beforeFailure.plusSeconds(25))
                .isBefore(afterFailure.plusSeconds(35));
    }

    @Test
    void shouldMoveMessageToDeadWhenMaximumAttemptsIsReached() {
        ClaimedRow claimedRow = persistAndClaimMessage(4, 5);

        int updatedRows = repository.recordFailure(
                claimedRow.messageId(),
                claimedRow.lockToken(),
                "Kafka remained unavailable",
                30L
        );

        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();

        OutboxMessageJpaEntity persisted = repository.findById(claimedRow.messageId())
                .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.DEAD);

        assertThat(persisted.getAttempts()).isEqualTo(5);
        assertThat(persisted.getLockedUntil()).isNull();
        assertThat(persisted.getLockToken()).isNull();
        assertThat(persisted.getNextAttemptAt()).isNull();
        assertThat(persisted.getProcessedAt()).isNull();

        assertThat(persisted.getFailureReason())
                .isEqualTo("Kafka remained unavailable");
    }

    @Test
    void shouldMarkProcessingMessageAsDeadForNonRetryableFailure() {
        ClaimedRow claimedRow = persistAndClaimMessage(0, 5);

        int updatedRows = repository.markAsDead(
                claimedRow.messageId(),
                claimedRow.lockToken(),
                "Invalid outbox payload"
        );

        assertThat(updatedRows).isEqualTo(1);

        entityManager.clear();

        OutboxMessageJpaEntity persisted = repository.findById(claimedRow.messageId())
                .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.DEAD);

        assertThat(persisted.getAttempts()).isEqualTo(1);
        assertThat(persisted.getLockedUntil()).isNull();
        assertThat(persisted.getLockToken()).isNull();
        assertThat(persisted.getNextAttemptAt()).isNull();
        assertThat(persisted.getProcessedAt()).isNull();

        assertThat(persisted.getFailureReason())
                .isEqualTo("Invalid outbox payload");
    }

    @Test
    void shouldNotMarkPendingMessageAsProcessed() {
        OutboxMessageJpaEntity pending = pendingMessage(
                OutboxChannel.EMAIL,
                Instant.now().minusSeconds(10)
        );

        repository.saveAndFlush(pending);
        entityManager.clear();

        int updatedRows = repository.markAsProcessed(pending.getId(), UUID.randomUUID());

        assertThat(updatedRows).isZero();

        entityManager.clear();

        OutboxMessageJpaEntity persisted = repository.findById(pending.getId())
                .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(OutboxStatus.PENDING);

        assertThat(persisted.getProcessedAt()).isNull();
    }

    private ClaimedRow persistAndClaimMessage(int attempts, int maxAttempts) {
        OutboxMessageJpaEntity pending = pendingMessage(
                OutboxChannel.EMAIL,
                Instant.now().minusSeconds(10),
                attempts,
                maxAttempts
        );

        repository.saveAndFlush(pending);
        entityManager.clear();

        List<OutboxMessageJpaEntity> claimed =
                repository.claimProcessableMessages(
                        OutboxChannel.EMAIL.name(),
                        OutboxEventType.ORDER_BILLED.name(),
                        1,
                        60L
                );

        assertThat(claimed).hasSize(1);

        OutboxMessageJpaEntity claimedMessage = claimed.getFirst();

        assertThat(claimedMessage.getId()).isEqualTo(pending.getId());
        assertThat(claimedMessage.getLockToken()).isNotNull();

        entityManager.clear();

        return new ClaimedRow(claimedMessage.getId(), claimedMessage.getLockToken());
    }

    private static OutboxMessageJpaEntity pendingMessage(OutboxChannel channel, Instant nextAttemptAt) {
        return pendingMessage(channel, nextAttemptAt, 0, 5);
    }

    private static OutboxMessageJpaEntity pendingMessage(
            OutboxChannel channel,
            Instant nextAttemptAt,
            int attempts,
            int maxAttempts
    ) {
        UUID messageId = UuidV7.generate();
        UUID invoiceId = UuidV7.generate();

        return new OutboxMessageJpaEntity(
                messageId,
                OutboxAggregateType.INVOICE,
                invoiceId.toString(),
                OutboxEventType.ORDER_BILLED,
                EVENT_VERSION,
                nextAttemptAt,
                channel,
                messageKeyFor(channel),
                UuidV7.generate().toString(),
                null,
                JsonNodeFactory.instance
                        .objectNode()
                        .put("invoiceId", invoiceId.toString())
                        .put("orderId", 123L),
                OutboxStatus.PENDING,
                attempts,
                maxAttempts,
                nextAttemptAt,
                idempotencyKey(channel, invoiceId),
                null
        );
    }

    /**
     * Espelha {@code chk_outbox_message_key}: só o canal de mensageria tem partição a escolher.
     */
    private static String messageKeyFor(OutboxChannel channel) {
        return channel == OutboxChannel.MESSAGING ? "123" : null;
    }

    private static String idempotencyKey(OutboxChannel channel, UUID invoiceId) {
        return "%s-order-billed-invoice-%s".formatted(
                channel.name().toLowerCase(),
                invoiceId
        );
    }

    private record ClaimedRow(UUID messageId, UUID lockToken) {
    }
}
