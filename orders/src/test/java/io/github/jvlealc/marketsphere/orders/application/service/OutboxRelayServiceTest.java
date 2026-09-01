package io.github.jvlealc.marketsphere.orders.application.service;

import io.github.jvlealc.marketsphere.orders.application.exception.UndeliverableOutboxMessageException;
import io.github.jvlealc.marketsphere.orders.application.identity.UuidV7;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.ClaimedOutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxAggregateType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxFailureReason;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxRelaySettings;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.SerializedOutboxPayload;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OutboxRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O relay é o ponto do módulo em que um erro não aparece como exceção, e sim como mensagem presa em
 * {@code PROCESSING} para sempre ou como evento entregue duas vezes. Estes testes cobrem cada uma dessas
 * formas de falhar em silêncio.
 * <p>
 * Sem Spring e sem banco: o serviço depende de uma porta e de um {@link Clock}, e ambos entram pelo
 * construtor.
 */
class OutboxRelayServiceTest {

    private static final Instant CLAIMED_AT = Instant.parse("2026-09-01T10:00:00Z");

    private static final Duration LOCK_DURATION = Duration.ofSeconds(60);
    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(25);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(30);
    private static final int BATCH_SIZE = 20;

    /** Com estes valores o prazo do lote é {@code claimedAt + 60s − 25s}, ou seja, 35 segundos. */
    private static final OutboxRelaySettings SETTINGS =
            new OutboxRelaySettings(BATCH_SIZE, LOCK_DURATION, DELIVERY_TIMEOUT, RETRY_DELAY);

    private static final Duration DEADLINE = LOCK_DURATION.minus(DELIVERY_TIMEOUT);

    private static final OutboxChannel CHANNEL = OutboxChannel.MESSAGING;
    private static final OutboxEventType EVENT_TYPE = OutboxEventType.ORDER_PAID;

    private OutboxRepositoryPort outboxRepository;
    private TestClock clock;
    private OutboxRelayService relayService;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepositoryPort.class);
        clock = new TestClock(CLAIMED_AT);
        relayService = new OutboxRelayService(outboxRepository, clock);

        when(outboxRepository.markAsProcessed(any(), any())).thenReturn(true);
        when(outboxRepository.recordFailure(any(), any(), any(), any())).thenReturn(true);
        when(outboxRepository.markAsDead(any(), any(), any())).thenReturn(true);
    }

    // -------------------------------------------------------------------- claim

    @Test
    void shouldClaimUsingChannelEventTypeAndConfiguredLimits() {
        givenClaimed();

        relay(message -> { });

        verify(outboxRepository).claimProcessableMessages(CHANNEL, EVENT_TYPE, BATCH_SIZE, LOCK_DURATION);
    }

    @Test
    void shouldDoNothingElse_whenThereIsNothingToClaim() {
        givenClaimed();

        List<UUID> delivered = new ArrayList<>();
        relay(message -> delivered.add(message.getId()));

        assertThat(delivered).isEmpty();
        verify(outboxRepository, never()).markAsProcessed(any(), any());
        verify(outboxRepository, never()).recordFailure(any(), any(), any(), any());
        verify(outboxRepository, never()).markAsDead(any(), any(), any());
    }

    // --------------------------------------------------------------- conclusões

    @Test
    void shouldMarkAsProcessed_whenDeliverySucceeds() {
        ClaimedOutboxMessage claimed = claimedMessage();
        givenClaimed(claimed);

        relay(message -> { });

        verify(outboxRepository).markAsProcessed(claimed.messageId(), claimed.lockToken());
        verify(outboxRepository, never()).recordFailure(any(), any(), any(), any());
    }

    /** O default é retentável: só vai a {@code DEAD} o que a porta declarou como terminal. */
    @Test
    void shouldRecordFailure_whenDeliveryFailsInARetryableWay() {
        ClaimedOutboxMessage claimed = claimedMessage();
        givenClaimed(claimed);

        relay(message -> {
            throw new IllegalStateException("broker unavailable");
        });

        verify(outboxRepository).recordFailure(
                eq(claimed.messageId()), eq(claimed.lockToken()), any(OutboxFailureReason.class), eq(RETRY_DELAY));
        verify(outboxRepository, never()).markAsDead(any(), any(), any());
    }

    @Test
    void shouldMarkAsDead_whenMessageIsUndeliverable() {
        ClaimedOutboxMessage claimed = claimedMessage();
        givenClaimed(claimed);

        relay(message -> {
            throw new UndeliverableOutboxMessageException(
                    "payload cannot be serialized", new IllegalArgumentException("unmapped field"));
        });

        verify(outboxRepository).markAsDead(
                eq(claimed.messageId()), eq(claimed.lockToken()), any(OutboxFailureReason.class));
        verify(outboxRepository, never()).recordFailure(any(), any(), any(), any());
    }

    /**
     * Perder o lease é operação normal sob concorrência, não erro: o worker registra e segue, sem retentar
     * — retentar sobrescreveria a decisão de quem hoje é o dono da linha.
     */
    @Test
    void shouldTreatLostLeaseAsNormalOperation() {
        givenClaimed(claimedMessage());
        when(outboxRepository.markAsProcessed(any(), any())).thenReturn(false);

        relay(message -> { });

        verify(outboxRepository, times(1)).markAsProcessed(any(), any());
        verify(outboxRepository, never()).recordFailure(any(), any(), any(), any());
    }

    // ----------------------------------------------------- isolamento por mensagem

    @Test
    void shouldKeepProcessingBatch_whenOneDeliveryFails() {
        ClaimedOutboxMessage first = claimedMessage();
        ClaimedOutboxMessage second = claimedMessage();
        givenClaimed(first, second);

        List<UUID> delivered = new ArrayList<>();
        relay(message -> {
            delivered.add(message.getId());
            if (message.getId().equals(first.messageId())) {
                throw new IllegalStateException("first one fails");
            }
        });

        assertThat(delivered).containsExactly(first.messageId(), second.messageId());
        verify(outboxRepository).recordFailure(eq(first.messageId()), any(), any(), any());
        verify(outboxRepository).markAsProcessed(second.messageId(), second.lockToken());
    }

    /**
     * Uma falha ao concluir não pode abortar o lote: a entrega já pode ter acontecido, e a linha volta pelo
     * ramo de lease expirado.
     */
    @Test
    void shouldKeepProcessingBatch_whenConclusionThrows() {
        ClaimedOutboxMessage first = claimedMessage();
        ClaimedOutboxMessage second = claimedMessage();
        givenClaimed(first, second);

        when(outboxRepository.markAsProcessed(eq(first.messageId()), any()))
                .thenThrow(new RuntimeException("connection reset"));

        List<UUID> delivered = new ArrayList<>();
        relay(message -> delivered.add(message.getId()));

        assertThat(delivered).containsExactly(first.messageId(), second.messageId());
        verify(outboxRepository).markAsProcessed(second.messageId(), second.lockToken());
    }

    // ------------------------------------------------------------ prazo do lote

    /**
     * O prazo é concedido ao lote inteiro no instante do claim, mas as mensagens são entregues em série: o
     * relógio da última começa a correr quando a primeira foi reivindicada. Parar no prazo é o que impede
     * que uma entrega comece com o lease prestes a expirar e o evento saia duas vezes.
     */
    @Test
    void shouldStopBatch_whenLeaseDeadlineIsReached() {
        givenClaimed(claimedMessage(), claimedMessage(), claimedMessage());

        Duration perDelivery = DEADLINE.dividedBy(2).plusSeconds(1);

        List<UUID> delivered = new ArrayList<>();
        relay(message -> {
            delivered.add(message.getId());
            clock.advance(perDelivery);
        });

        assertThat(delivered).hasSize(2);
        verify(outboxRepository, times(2)).markAsProcessed(any(), any());
    }

    @Test
    void shouldDeliverWholeBatch_whileLeaseStillCoversIt() {
        givenClaimed(claimedMessage(), claimedMessage(), claimedMessage());

        List<UUID> delivered = new ArrayList<>();
        relay(message -> {
            delivered.add(message.getId());
            clock.advance(Duration.ofSeconds(1));
        });

        assertThat(delivered).hasSize(3);
        verify(outboxRepository, times(3)).markAsProcessed(any(), any());
    }

    // ------------------------------------------------------------------ helpers

    private void relay(OutboxRelayService.OutboxMessageDelivery delivery) {
        relayService.relay(CHANNEL, EVENT_TYPE, SETTINGS, delivery);
    }

    private void givenClaimed(ClaimedOutboxMessage... messages) {
        when(outboxRepository.claimProcessableMessages(any(), any(), anyInt(), any()))
                .thenReturn(List.of(messages));
    }

    private static ClaimedOutboxMessage claimedMessage() {
        OutboxMessage message = OutboxMessage.createNew(
                OutboxAggregateType.ORDER,
                "100",
                EVENT_TYPE,
                1,
                CLAIMED_AT,
                CHANNEL,
                "100",
                new SerializedOutboxPayload("{\"orderId\":100}"),
                "messaging-order-paid-" + UuidV7.generate(),
                EventLineage.start(),
                CLAIMED_AT
        );

        return new ClaimedOutboxMessage(message, UUID.randomUUID());
    }

    /** Relógio controlado pelo teste: permite que a entrega "gaste" tempo do lease sem que o teste durma. */
    private static final class TestClock extends Clock {

        private Instant instant;

        private TestClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration amount) {
            instant = instant.plus(amount);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
