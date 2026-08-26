package io.github.jvlealc.marketsphere.orders.application.model.outbox;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Mensagem reivindicada por um worker, acompanhada do token que prova a posse do <em>lease</em>.
 *
 * <h4>Por que o token não mora em {@link OutboxMessage}</h4>
 * Ele não descreve o evento — descreve quem, agora, tem o direito de concluí-lo. É estado efêmero de
 * coordenação entre workers, e some assim que a mensagem sai de {@code PROCESSING}. Misturá-lo ao envelope
 * do evento faria o modelo carregar um campo que é nulo em toda linha que não está sendo processada.
 *
 * <h4>Para que ele serve</h4>
 * Todas as três conclusões: {@code markAsProcessed}, {@code recordFailure} e {@code markAsDead}, exigem o
 * token. A guarda {@code status = 'PROCESSING'} sozinha não basta: ela impede que um worker atrasado
 * altere uma linha <em>já concluída</em> por outro, mas não que ele interfira enquanto o outro
 * <em>ainda está publicando</em>. Nessa janela, sem token, o worker atrasado marcaria como
 * {@code PROCESSED} algo que ainda não saiu, culminando em perda silenciosa ou devolveria a {@code FAILED} uma linha
 * publicada com sucesso, agendando a republicação de um evento que já foi entregue.
 */
public record ClaimedOutboxMessage(OutboxMessage message, UUID lockToken) {
    public ClaimedOutboxMessage {
        requireNonNull(message, "Outbox message must not be null");
        requireNonNull(lockToken, "Lock token must not be null");
    }

    public UUID messageId() {
        return message.getId();
    }
}
