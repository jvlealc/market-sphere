package io.github.jvlealc.marketsphere.billing.application.service;

import io.github.jvlealc.marketsphere.billing.application.exception.UndeliverableOutboxMessageException;
import io.github.jvlealc.marketsphere.billing.application.identity.UuidV7;
import io.github.jvlealc.marketsphere.billing.application.model.messaging.EventLineage;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.ClaimedOutboxMessage;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxAggregateType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxFailureReason;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.SerializedOutboxPayload;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxRelaySettings;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OutboxRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * O relay é o ponto do módulo em que um erro não aparece como exceção, e sim como mensagem que fica presa
 * em {@code PROCESSING} para sempre ou como evento entregue duas vezes. Estes testes cobrem cada uma dessas
 * formas de falhar em silêncio.
 * <p>
 * Sem Spring e sem banco: o serviço depende de uma porta e de um {@link Clock}, e ambos entram pelo
 * construtor.
 */
class OutboxRelayServiceTest {

    private static final Instant CLAIMED_AT = Instant.parse("2026-08-02T10:00:00Z");
    private static final Duration LOCK_DURATION = Duration.ofSeconds(60);
    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(25);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(10);
    private static final int BATCH_SIZE = 20;

    private static final OutboxRelaySettings SETTINGS =
            new OutboxRelaySettings(BATCH_SIZE, LOCK_DURATION, DELIVERY_TIMEOUT, RETRY_DELAY);

    /** Cada mensagem precisa de uma chave de idempotência distinta, como no banco. */
    private static final AtomicInteger IDEMPOTENCY_SEQUENCE = new AtomicInteger();

    private OutboxRepositoryPort outboxRepository;
    private MutableClock clock;
    private OutboxRelayService relayService;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepositoryPort.class);
        clock = new MutableClock(CLAIMED_AT);
        relayService = new OutboxRelayService(outboxRepository, clock);
    }

    @Test
    void shouldClaimUsingTheChannelEventTypeAndConfiguredLimits() {
        givenClaimedMessages();

        relay();

        verify(outboxRepository).claimProcessableMessages(
                OutboxChannel.MESSAGING,
                OutboxEventType.ORDER_BILLED,
                BATCH_SIZE,
                LOCK_DURATION
        );
    }

    @Test
    void shouldDoNothingElseWhenThereIsNothingToClaim() {
        givenClaimedMessages();

        List<OutboxMessage> delivered = relay();

        assertThat(delivered).isEmpty();
        verify(outboxRepository).claimProcessableMessages(any(), any(), anyInt(), any());
        verifyNoMoreInteractions(outboxRepository);
    }

    @Test
    void shouldMarkAsProcessedWhenDeliverySucceeds() {
        ClaimedOutboxMessage claimed = claimedMessage();
        givenClaimedMessages(claimed);
        givenConclusionSucceeds();

        List<OutboxMessage> delivered = relay();

        // O envelope inteiro chega ao entregador: o canal de mensageria precisa dos metadados para os headers.
        assertThat(delivered).containsExactly(claimed.message());
        verify(outboxRepository).markAsProcessed(claimed.messageId(), claimed.lockToken());
        verify(outboxRepository, never()).recordFailure(any(), any(), any(), any());
        verify(outboxRepository, never()).markAsDead(any(), any(), any());
    }

    @Test
    void shouldRecordFailureWhenDeliveryFailsInARetryableWay() {
        ClaimedOutboxMessage claimed = claimedMessage();
        givenClaimedMessages(claimed);
        givenConclusionSucceeds();

        relayFailingWith(new IllegalStateException("broker is down"));

        ArgumentCaptor<OutboxFailureReason> reason = ArgumentCaptor.forClass(OutboxFailureReason.class);

        verify(outboxRepository).recordFailure(
                eq(claimed.messageId()),
                eq(claimed.lockToken()),
                reason.capture(),
                eq(RETRY_DELAY)
        );
        verify(outboxRepository, never()).markAsProcessed(any(), any());
        verify(outboxRepository, never()).markAsDead(any(), any(), any());

        // O motivo precisa identificar a falha; uma frase genérica tornaria a coluna inútil.
        assertThat(reason.getValue().value()).contains("IllegalStateException", "broker is down");
    }

    @Test
    void shouldMarkAsDeadWhenTheMessageIsUndeliverable() {
        ClaimedOutboxMessage claimed = claimedMessage();
        givenClaimedMessages(claimed);
        givenConclusionSucceeds();

        relayFailingWith(new UndeliverableOutboxMessageException(
                "payload cannot be read",
                new IllegalArgumentException("Order ID is required")
        ));

        // Falha de contrato não deve consumir a fila de tentativas para chegar ao mesmo DEAD.
        verify(outboxRepository).markAsDead(eq(claimed.messageId()), eq(claimed.lockToken()), any());
        verify(outboxRepository, never()).recordFailure(any(), any(), any(), any());
        verify(outboxRepository, never()).markAsProcessed(any(), any());
    }

    @Test
    void shouldTreatALostLeaseAsNormalOperationAndNotAsAnError() {
        ClaimedOutboxMessage claimed = claimedMessage();
        givenClaimedMessages(claimed);
        // false = outro worker reivindicou a linha enquanto esta entrega acontecia.
        when(outboxRepository.markAsProcessed(any(), any())).thenReturn(false);

        List<OutboxMessage> delivered = relay();

        assertThat(delivered).hasSize(1);
        verify(outboxRepository).claimProcessableMessages(any(), any(), anyInt(), any());
        verify(outboxRepository).markAsProcessed(claimed.messageId(), claimed.lockToken());
        // Nunca retentar: quem tem o lease agora é quem decide o desfecho.
        verifyNoMoreInteractions(outboxRepository);
    }

    @Test
    void shouldKeepProcessingTheBatchWhenOneDeliveryFails() {
        ClaimedOutboxMessage first = claimedMessage();
        ClaimedOutboxMessage poisoned = claimedMessage();
        ClaimedOutboxMessage last = claimedMessage();
        givenClaimedMessages(first, poisoned, last);
        givenConclusionSucceeds();

        List<OutboxMessage> delivered = new ArrayList<>();

        relayService.relay(OutboxChannel.MESSAGING, OutboxEventType.ORDER_BILLED, SETTINGS, message -> {
            delivered.add(message);

            if (message.getId().equals(poisoned.messageId())) {
                throw new IllegalStateException("this one blows up");
            }
        });

        // Uma exceção escapando do laço deixaria as duas seguintes presas em PROCESSING sem terem sido tentadas.
        assertThat(delivered).containsExactly(first.message(), poisoned.message(), last.message());
        verify(outboxRepository).markAsProcessed(first.messageId(), first.lockToken());
        verify(outboxRepository).markAsProcessed(last.messageId(), last.lockToken());
        verify(outboxRepository).recordFailure(eq(poisoned.messageId()), any(), any(), any());
    }

    @Test
    void shouldKeepProcessingTheBatchWhenAConclusionFails() {
        ClaimedOutboxMessage first = claimedMessage();
        ClaimedOutboxMessage last = claimedMessage();
        givenClaimedMessages(first, last);

        when(outboxRepository.markAsProcessed(first.messageId(), first.lockToken()))
                .thenThrow(new IllegalStateException("connection pool exhausted"));
        when(outboxRepository.markAsProcessed(last.messageId(), last.lockToken()))
                .thenReturn(true);

        List<OutboxMessage> delivered = relay();

        // A entrega já aconteceu; propagar aqui abortaria o restante do lote sem desfazer nada.
        assertThat(delivered).containsExactly(first.message(), last.message());
        verify(outboxRepository).markAsProcessed(last.messageId(), last.lockToken());
    }

    @Test
    void shouldStopTheBatchWhenTheRemainingLeaseNoLongerCoversADelivery() {
        ClaimedOutboxMessage first = claimedMessage();
        ClaimedOutboxMessage second = claimedMessage();
        ClaimedOutboxMessage third = claimedMessage();
        givenClaimedMessages(first, second, third);
        givenConclusionSucceeds();

        List<OutboxMessage> delivered = new ArrayList<>();

        // A primeira entrega consome quase to-do o lease: o prazo é claimedAt + 60s - 25s = 35s.
        relayService.relay(OutboxChannel.MESSAGING, OutboxEventType.ORDER_BILLED, SETTINGS, message -> {
            delivered.add(message);
            clock.advance(Duration.ofSeconds(40));
        });

        // Só a primeira sai. As outras duas continuam em PROCESSING com o lease prestes a expirar e voltam
        // pelo ramo de reivindicação de lease expirado — publicá-las agora arriscaria evento duplicado.
        assertThat(delivered).containsExactly(first.message());
        verify(outboxRepository).markAsProcessed(first.messageId(), first.lockToken());
        verify(outboxRepository, never()).markAsProcessed(second.messageId(), second.lockToken());
        verify(outboxRepository, never()).markAsProcessed(third.messageId(), third.lockToken());
    }

    @Test
    void shouldDeliverTheWholeBatchWhileTheLeaseStillCoversIt() {
        ClaimedOutboxMessage first = claimedMessage();
        ClaimedOutboxMessage second = claimedMessage();
        givenClaimedMessages(first, second);
        givenConclusionSucceeds();

        List<OutboxMessage> delivered = new ArrayList<>();

        relayService.relay(OutboxChannel.MESSAGING, OutboxEventType.ORDER_BILLED, SETTINGS, message -> {
            delivered.add(message);
            clock.advance(Duration.ofSeconds(5));
        });

        // O lote grande só é seguro porque existe a parada por prazo; dentro do prazo ele deve sair inteiro.
        assertThat(delivered).containsExactly(first.message(), second.message());
        verify(outboxRepository, times(2)).markAsProcessed(any(), any());
    }

    // ---------------------------------------------------------------------------------------------------

    private List<OutboxMessage> relay() {
        List<OutboxMessage> delivered = new ArrayList<>();

        relayService.relay(OutboxChannel.MESSAGING, OutboxEventType.ORDER_BILLED, SETTINGS, delivered::add);

        return delivered;
    }

    private void relayFailingWith(RuntimeException failure) {
        relayService.relay(OutboxChannel.MESSAGING, OutboxEventType.ORDER_BILLED, SETTINGS, message -> {
            throw failure;
        });
    }

    private void givenClaimedMessages(ClaimedOutboxMessage... messages) {
        when(outboxRepository.claimProcessableMessages(any(), any(), anyInt(), any()))
                .thenReturn(List.of(messages));
    }

    private void givenConclusionSucceeds() {
        when(outboxRepository.markAsProcessed(any(), any())).thenReturn(true);
        when(outboxRepository.recordFailure(any(), any(), any(), any())).thenReturn(true);
        when(outboxRepository.markAsDead(any(), any(), any())).thenReturn(true);
    }

    private static ClaimedOutboxMessage claimedMessage() {
        OutboxMessage message = OutboxMessage.createNew(
                OutboxAggregateType.INVOICE,
                UuidV7.generate().toString(),
                OutboxEventType.ORDER_BILLED,
                1,
                CLAIMED_AT,
                OutboxChannel.MESSAGING,
                "42",
                new SerializedOutboxPayload("{\"orderId\":42}"),
                "messaging-order-billed-" + IDEMPOTENCY_SEQUENCE.incrementAndGet(),
                EventLineage.from(null, null),
                CLAIMED_AT
        );

        return new ClaimedOutboxMessage(message, UUID.randomUUID());
    }

    /**
     * Relógio controlado pelo teste. Permite que a entrega "gaste" tempo do lease sem que o teste durma.
     */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
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
