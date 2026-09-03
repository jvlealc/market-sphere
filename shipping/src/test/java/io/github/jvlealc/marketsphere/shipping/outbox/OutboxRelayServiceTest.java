package io.github.jvlealc.marketsphere.shipping.outbox;

import io.github.jvlealc.marketsphere.shipping.identity.UuidV7;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O relay é o ponto do módulo em que um erro não aparece como exceção, e sim como mensagem presa em
 * {@code PROCESSING} ou como evento entregue duas vezes. Estes testes cobrem cada uma dessas formas de
 * falhar em silêncio.
 * <p>
 * O prazo do lote não é conferido aqui de propósito: a garantia de que ele cobre
 * {@code batchSize x deliveryTimeout} é invariante de {@link OutboxRelayProps}, e é lá que está testada.
 */
class OutboxRelayServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    private static final int BATCH_SIZE = 10;
    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration CLAIM_DURATION = Duration.ofSeconds(30);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(10);

    private static final OutboxRelayProps PROPS = new OutboxRelayProps(
            BATCH_SIZE, DELIVERY_TIMEOUT, CLAIM_DURATION, RETRY_DELAY, 2.0, Duration.ofMinutes(1));

    private OutboxJpaRepository repository;
    private KafkaOutboxPublisher publisher;
    private OutboxRelayService relayService;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxJpaRepository.class);
        publisher = mock(KafkaOutboxPublisher.class);
        relayService = new OutboxRelayService(
                repository, publisher, PROPS, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ------------------------------------------------------------ reivindicação

    @Test
    void shouldClaimUsingConfiguredBatchSizeAndDeadline() {
        givenClaimed();

        relayService.relayPendingMessages();

        verify(repository).claimProcessableMessages(BATCH_SIZE, CLAIM_DURATION.toSeconds());
    }

    @Test
    void shouldDoNothingElse_whenThereIsNothingToClaim() {
        givenClaimed();

        relayService.relayPendingMessages();

        verifyNoInteractions(publisher);
        verify(repository, never()).markAsProcessed(any());
        verify(repository, never()).recordFailure(any(), any(), any());
        verify(repository, never()).markAsDead(any(), any());
    }

    @Test
    void shouldPublishWithConfiguredDeliveryTimeout() {
        OutboxMessage message = claimedMessage(0);
        givenClaimed(message);

        relayService.relayPendingMessages();

        verify(publisher).publish(message, DELIVERY_TIMEOUT);
    }

    // ---------------------------------------------------------------- desfechos

    @Test
    void shouldMarkAsProcessed_whenDeliverySucceeds() {
        OutboxMessage message = claimedMessage(0);
        givenClaimed(message);

        relayService.relayPendingMessages();

        verify(repository).markAsProcessed(message.getId());
        verify(repository, never()).recordFailure(any(), any(), any());
        verify(repository, never()).markAsDead(any(), any());
    }

    @Test
    void shouldMarkAsDead_whenFailureIsTerminal() {
        OutboxMessage message = claimedMessage(0);
        givenClaimed(message);
        givenDeliveryFails(new UndeliverableOutboxMessageException("payload is unreadable", null));

        relayService.relayPendingMessages();

        verify(repository).markAsDead(
                message.getId(), "UndeliverableOutboxMessageException: payload is unreadable");
        verify(repository, never()).recordFailure(any(), any(), any());
        verify(repository, never()).markAsProcessed(any());
    }

    @Test
    void shouldRecordFailureAndScheduleNextAttempt_whenFailureIsRetryable() {
        OutboxMessage message = claimedMessage(0);
        givenClaimed(message);
        givenDeliveryFails(new OutboxDeliveryException("broker is unreachable", null));

        relayService.relayPendingMessages();

        verify(repository).recordFailure(
                message.getId(), "OutboxDeliveryException: broker is unreachable", NOW.plus(RETRY_DELAY));
        verify(repository, never()).markAsDead(any(), any());
        verify(repository, never()).markAsProcessed(any());
    }

    @Test
    void shouldScheduleNextAttemptFromAttemptsAlreadyMade() {
        OutboxMessage message = claimedMessage(2);
        givenClaimed(message);
        givenDeliveryFails(new OutboxDeliveryException("broker is unreachable", null));

        relayService.relayPendingMessages();

        verify(repository).recordFailure(eq(message.getId()), any(), eq(NOW.plus(Duration.ofSeconds(40))));
    }

    /**
     * Só se marca como terminal o que se sabe classificar. Um defeito de programação gasta as tentativas
     * até {@code DEAD}, mas aparece em {@code failure_reason} com nome e mensagem, em vez de girar num
     * laço silencioso.
     */
    @Test
    void shouldRetryFailuresItCannotClassify() {
        OutboxMessage message = claimedMessage(0);
        givenClaimed(message);
        givenDeliveryFails(new IllegalStateException("null aggregate id"));

        relayService.relayPendingMessages();

        verify(repository).recordFailure(
                eq(message.getId()), eq("IllegalStateException: null aggregate id"), any());
        verify(repository, never()).markAsDead(any(), any());
    }

    // -------------------------------------------------------- resiliência do lote

    @Test
    void shouldKeepProcessingBatch_whenOneDeliveryFails() {
        OutboxMessage failing = claimedMessage(0);
        OutboxMessage next = claimedMessage(0);
        givenClaimed(failing, next);

        doThrow(new OutboxDeliveryException("broker is unreachable", null))
                .when(publisher).publish(eq(failing), any());

        relayService.relayPendingMessages();

        verify(publisher).publish(next, DELIVERY_TIMEOUT);
        verify(repository).recordFailure(eq(failing.getId()), any(), any());
        verify(repository).markAsProcessed(next.getId());
    }

    /**
     * Zero linhas afetadas significa que o prazo expirou e outro worker assumiu a mensagem. É operação
     * normal: registra-se e segue, porque retentar sobrescreveria a decisão de quem hoje é o dono dela.
     */
    @Test
    void shouldCarryOn_whenClaimWasAlreadyTakenOver() {
        OutboxMessage message = claimedMessage(0);
        OutboxMessage next = claimedMessage(0);
        givenClaimed(message, next);

        when(repository.markAsProcessed(message.getId())).thenReturn(0);

        assertThatCode(() -> relayService.relayPendingMessages()).doesNotThrowAnyException();

        verify(repository).markAsProcessed(next.getId());
    }

    /**
     * A entrega pode já ter acontecido quando a conclusão falha. Propagar abortaria o restante do lote;
     * a linha fica em {@code PROCESSING} até o prazo expirar e é publicada de novo, por isso o consumidor
     * precisa ser idempotente.
     */
    @Test
    void shouldKeepProcessingBatch_whenConclusionFails() {
        OutboxMessage message = claimedMessage(0);
        OutboxMessage next = claimedMessage(0);
        givenClaimed(message, next);

        when(repository.markAsProcessed(message.getId()))
                .thenThrow(new RuntimeException("connection reset"));

        assertThatCode(() -> relayService.relayPendingMessages()).doesNotThrowAnyException();

        verify(publisher).publish(next, DELIVERY_TIMEOUT);
        verify(repository).markAsProcessed(next.getId());
    }

    // ---------------------------------------------------------------- fixtures

    private void givenClaimed(OutboxMessage... messages) {
        when(repository.claimProcessableMessages(anyInt(), anyLong())).thenReturn(List.of(messages));
    }

    private void givenDeliveryFails(RuntimeException failure) {
        doThrow(failure).when(publisher).publish(any(), any());
    }

    private static OutboxMessage claimedMessage(int attemptsAlreadyMade) {
        OutboxMessage message = mock(OutboxMessage.class);

        when(message.getId()).thenReturn(UuidV7.generate());
        when(message.getAttempts()).thenReturn(attemptsAlreadyMade);

        return message;
    }
}
