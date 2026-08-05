package io.github.jvlealc.marketsphere.billing.application.ports.out;

import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxMessage;

/**
 * Publica o evento {@code ORDER_BILLED} para os demais serviços.
 * <p>
 * Recebe a <strong>mensagem</strong> de outbox, não um record de evento: o evento é o {@code payload} dela,
 * e o adapter o publica <em>verbatim</em>, sem desserializar. Esse JSON foi congelado no instante da
 * transação de negócio — desserializar e re-serializar faria uma mudança de código alterar o conteúdo de
 * linhas gravadas antes dessa mudança, anulando a garantia que a outbox existe para dar.
 * <p>
 * Uma porta por tipo de evento, e não uma porta genérica de mensageria: quem reivindica as mensagens já
 * escolhe o par {@code (canal, tipo)} em
 * {@link OutboxRepositoryPort#claimProcessableMessages}, então o roteamento por tipo já está resolvido
 * antes daqui. Uma porta genérica só devolveria essa decisão ao worker, e em troca de um nome que não
 * diz o que é publicado.
 */
public interface OrderBilledPublisherPort {

    /**
     * @throws io.github.jvlealc.marketsphere.billing.application.exception.OutboxDeliveryException
     *         se a publicação falhar de forma retentável
     */
    void publish(OutboxMessage message);
}
