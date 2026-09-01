package io.github.jvlealc.marketsphere.shipping.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * Reivindicar, entregar, concluir.
 * <p>
 * O método do lote <strong>não</strong> é transacional de propósito: uma transação em volta dele
 * seguraria uma conexão do pool durante cada publicação no Kafka. As fronteiras ficam nas escritas
 * do repositório, uma transação por mensagem.
 */
@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxJpaRepository outboxJpaRepository;
    private final KafkaOutboxPublisher publisher;
    private final OutboxRelayProps props;
    private final Clock clock;

    OutboxRelayService(
            OutboxJpaRepository outboxJpaRepository,
            KafkaOutboxPublisher publisher,
            OutboxRelayProps props,
            Clock clock
    ) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.publisher = publisher;
        this.props = props;
        this.clock = clock;
    }

    public void relayPendingMessages() {
        List<OutboxMessage> claimed = outboxJpaRepository.claimProcessableMessages(
                props.batchSize(),
                props.claimDuration().toSeconds()
        );

        for (OutboxMessage message : claimed) {
            deliverAndConclude(message);
        }
    }

    private void deliverAndConclude(OutboxMessage message) {
        UUID messageId = message.getId();

        try {
            publisher.publish(message, props.deliveryTimeout());

        } catch (UndeliverableOutboxMessageException terminalFailure) {
            log.error("Outbox message {} is undeliverable and will be marked as DEAD.", messageId, terminalFailure);

            conclude(
                    () -> outboxJpaRepository.markAsDead(messageId, OutboxFailureReason.of(terminalFailure).value()),
                    messageId,
                    "markAsDead"
            );
            return;

        } catch (Exception retryableFailure) {
            // Só se marca como terminal o que se sabe classificar. O não classificado é retentado:
            // um defeito de programação gasta as tentativas até DEAD, mas aparece em failure_reason
            // com nome e mensagem, em vez de girar num laço silencioso.
            Instant nextAttemptAt = Instant.now(clock).plus(props.backoffFor(message.getAttempts()));

            log.warn("Delivery of outbox message {} failed; next attempt at {}.", messageId, nextAttemptAt, retryableFailure);

            conclude(
                    () -> outboxJpaRepository.recordFailure(
                            messageId,
                            OutboxFailureReason.of(retryableFailure).value(),
                            nextAttemptAt
                    ),
                    messageId,
                    "recordFailure"
            );
            return;
        }

        conclude(() -> outboxJpaRepository.markAsProcessed(messageId), messageId, "markAsProcessed");
    }

    /**
     * Zero linhas afetadas significa que o prazo da reivindicação expirou e outro worker já assumiu a
     * mensagem. É operação normal, não erro: registra-se e segue. Retentar aqui sobrescreveria a
     * decisão de quem hoje é o dono dela.
     * <p>
     * Zero recorrente é o sintoma de {@code claimDuration} subdimensionado em relação a
     * {@code batchSize x deliveryTimeout}, e é a razão de o retorno existir.
     */
    private void conclude(IntSupplier conclusion, UUID messageId, String operation) {
        try {
            if (conclusion.getAsInt() == 0) {
                log.info("Outbox message {} was no longer PROCESSING before {}. Nothing to do.", messageId, operation);
            }
        } catch (Exception persistenceFailure) {
            // A entrega pode já ter acontecido. Falhar aqui deixa a linha em PROCESSING até o prazo
            // expirar, e daí ela é publicada de novo — por isso o consumidor precisa ser idempotente.
            // Propagar seria pior: abortaria o restante do lote.
            log.error("Could not run {} for outbox message {}.", operation, messageId, persistenceFailure);
        }
    }
}
