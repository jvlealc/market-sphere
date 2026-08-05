package io.github.jvlealc.marketsphere.billing.application.service;

import io.github.jvlealc.marketsphere.billing.application.exception.UndeliverableOutboxMessageException;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.ClaimedOutboxMessage;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxFailureReason;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxRelaySettings;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OutboxRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * O algoritmo de relay da outbox: reivindicar, entregar, concluir. Um por par {@code (canal, tipo)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayService {

    private final OutboxRepositoryPort outboxRepository;

    /**
     * Injetado em vez de {@code Instant.now()} estático porque a parada por prazo de lease é a regra mais
     * sutil desta classe, e a única forma de verificá-la com relógio real seria dormir dentro do teste —
     * trocando determinismo por tempo de build.
     */
    private final Clock clock;

    /**
     * Recebe o envelope inteiro porque o canal de mensageria
     * publica o payload verbatim e precisa dos metadados para montar os headers.
     * <p>
     * Deve lançar {@link UndeliverableOutboxMessageException} quando repetir não muda o resultado. Qualquer
     * outra exceção é tratada como retentável.
     */
    @FunctionalInterface
    public interface OutboxMessageDelivery {

        void deliver(OutboxMessage message);
    }

    public void relay(
            OutboxChannel channel,
            OutboxEventType eventType,
            OutboxRelaySettings settings,
            OutboxMessageDelivery delivery
    ) {
        Instant claimedAt = Instant.now(clock);

        List<ClaimedOutboxMessage> claimedMessages = outboxRepository.claimProcessableMessages(
                channel,
                eventType,
                settings.batchSize(),
                settings.lockDuration()
        );

        if (claimedMessages.isEmpty()) {
            return;
        }

        Instant deadline = settings.deadlineFrom(claimedAt);

        for (int index = 0; index < claimedMessages.size(); index++) {
            if (Instant.now(clock).isAfter(deadline)) {
                // As não processadas continuam em PROCESSING com o lease prestes a expirar: voltam sozinhas
                // pelo ramo de reivindicação de lease expirado, na próxima rodada. Interromper aqui é o que
                // permite manter o lote grande sem que ele vire fonte de evento duplicado.
                log.warn(
                        "Lease deadline reached for the {}/{} outbox relay. Stopping the batch with {} of {} messages left for the next round.",
                        channel, eventType, claimedMessages.size() - index, claimedMessages.size()
                );
                return;
            }

            deliverAndConclude(claimedMessages.get(index), settings, delivery);
        }
    }

    private void deliverAndConclude(
            ClaimedOutboxMessage claimed,
            OutboxRelaySettings settings,
            OutboxMessageDelivery delivery
    ) {
        UUID messageId = claimed.messageId();
        UUID lockToken = claimed.lockToken();

        try {
            delivery.deliver(claimed.message());

        } catch (UndeliverableOutboxMessageException terminalFailure) {
            log.error("Outbox message {} is undeliverable and will be marked as DEAD.", messageId, terminalFailure);

            conclude(
                    () -> outboxRepository.markAsDead(messageId, lockToken, OutboxFailureReason.of(terminalFailure)),
                    messageId,
                    "markAsDead"
            );
            return;

        } catch (Exception retryableFailure) {
            log.warn("Delivery of outbox message {} failed and will be retried.", messageId, retryableFailure);

            conclude(() -> outboxRepository.recordFailure(
                            messageId,
                            lockToken,
                            OutboxFailureReason.of(retryableFailure),
                            settings.retryDelay()
                    ),
                    messageId,
                    "recordFailure"
            );
            return;
        }

        conclude(() -> outboxRepository.markAsProcessed(messageId, lockToken), messageId, "markAsProcessed");
    }

    /**
     * Executa a conclusão e interpreta o {@code boolean} da porta.
     * <p>
     * {@code false} significa que este worker perdeu o lease enquanto entregava — outro já reivindicou a
     * linha. É operação normal sob concorrência, e não erro: registra-se e segue. Nunca retentar aqui, sob
     * pena de sobrescrever a decisão de quem hoje é o dono da mensagem.
     * <p>
     * Um {@code false} recorrente é o sintoma de que {@code deliveryTimeout} está subdimensionado em
     * relação ao {@code lockDuration}, e é a razão de o retorno existir: descartá-lo apagaria o único
     * instrumento capaz de revelar isso em produção.
     */
    private void conclude(BooleanSupplier conclusion, UUID messageId, String operation) {
        try {
            if (!conclusion.getAsBoolean()) {
                log.info(
                        "Lost the lease on outbox message {} before {}; another worker owns it now. Nothing to do.",
                        messageId, operation
                );
            }

        } catch (Exception persistenceFailure) {
            // A entrega pode já ter acontecido. Falhar aqui deixa a linha em PROCESSING até o lease expirar,
            // e daí ela é reivindicada de novo, por isso o consumidor precisa ser idempotente. Propagar
            // seria pior: abortaria o restante do lote.
            log.error("Could not run {} for outbox message {}.", operation, messageId, persistenceFailure);
        }
    }
}
