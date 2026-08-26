package io.github.jvlealc.marketsphere.orders.application.ports.out;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso à outbox transacional.
 *
 * <p>Sobre o {@code boolean} de retorno das conclusões</p>
 * As três conclusões devolvem se a atualização <em>pegou</em>. {@code false} significa que este worker
 * perdeu o lease — outro reivindicou a mensagem depois do {@code lockedUntil} expirar — e portanto não tem
 * mais o direito de decidir o desfecho dela. Isso é operação normal sob concorrência, não erro: o chamador
 * deve registrar e seguir adiante, nunca retentar nem lançar.
 */
public interface OutboxRepositoryPort {

    void appendNew(OutboxMessage message);

    /**
     * Reivindica mensagens processáveis, movendo-as para {@code PROCESSING} com um lease de
     * {@code lockDuration} e um token de posse recém-gerado.
     * <p>
     * O {@code lockDuration} precisa ser maior que o tempo máximo que a publicação pode levar. Se o lease
     * expirar antes, outro worker reivindica a mesma linha e publica em paralelo.
     */
    List<ClaimedOutboxMessage> claimProcessableMessages(
            OutboxChannel channel,
            OutboxEventType eventType,
            int limit,
            Duration lockDuration
    );

    boolean markAsProcessed(UUID messageId, UUID lockToken);


    /**
     * Registra uma falha retentável. A promoção para {@code DEAD} ao esgotar {@code maxAttempts} é decidida
     * no próprio {@code UPDATE} — o chamador não conta tentativas, sob pena de duplicar a regra.
     */
    boolean recordFailure(
            UUID messageId,
            UUID lockToken,
            OutboxFailureReason failureReason,
            Duration retryDelay
    );

    /**
     * Encerra a mensagem sem retentativa. Para falhas em que tentar de novo não muda o resultado —
     * payload ilegível, contrato violado —, que não devem consumir a fila de tentativas.
     */
    boolean markAsDead(UUID messageId, UUID lockToken, OutboxFailureReason failureReason);
}
