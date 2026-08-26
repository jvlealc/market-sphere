package io.github.jvlealc.marketsphere.orders.application.model.outbox;

import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * Parâmetros de operação de um worker de relay da outbox.
 * <p>
 * Eles não são configurações independentes: existe uma relação entre {@code deliveryTimeout} e
 * {@code lockDuration} que, se violada, produz <strong>evento duplicado</strong>. Estarem num objeto só é o
 * que permite validá-la <em>no boot</em>, e não em produção.
 *
 * <h4>A invariante</h4>
 * {@code deliveryTimeout < lockDuration}, com margem. O lease precisa sobreviver a uma entrega que consome
 * completamente o orçamento dela; se expirar antes, outro worker reivindica a mesma linha e publica em paralelo. O
 * {@code lockToken} protege a <em>conclusão</em>, nunca a <em>publicação</em>: o worker atrasado descobre
 * que perdeu o lease só depois de o evento já ter saído duas vezes.
 * <p>
 * O {@code batchSize} <strong>não</strong> entra na invariante porque
 * {@code OutboxRelayService} interrompe o lote quando o lease restante fica menor que
 * {@code deliveryTimeout}. Sem essa parada, a relação necessária seria
 * {@code batchSize × deliveryTimeout < lockDuration} — com lote de 20 e 25s de timeout, isso exigiria um
 * lease de mais de 8 minutos, que é tempo demais para uma mensagem ficar parada depois de um {@code kill -9}.
 * O lote passa a ser botão de vazão, não de correção.
 */
public record OutboxRelaySettings(
        int batchSize,
        Duration lockDuration,
        Duration deliveryTimeout,
        Duration retryDelay
) {

    public OutboxRelaySettings {
        requireNonNull(lockDuration, "Lock duration must not be null");
        requireNonNull(deliveryTimeout, "Delivery timeout must not be null");
        requireNonNull(retryDelay, "Retry delay must not be null");

        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than zero");
        }

        requiredPositive(lockDuration, "Lock duration");
        requiredPositive(deliveryTimeout, "Delivery timeout");
        requiredPositive(retryDelay, "Retry delay");

        if (deliveryTimeout.compareTo(lockDuration) >= 0) {
            throw new IllegalArgumentException("""
                    Delivery timeout (%s) must be shorter than the lock duration (%s), otherwise the lease can expire
                    while the message is still being delivered and another worker will publish it again
                    """
                    .formatted(deliveryTimeout, lockDuration)
            );
        }
    }

    /**
     * Instante a partir do qual não se deve iniciar uma nova entrega neste lote: o que restar do lease já
     * não cobre um {@code deliveryTimeout} inteiro.
     * <p>
     * O {@code claimedAt} é medido no relógio da JVM <em>antes</em> da chamada de reivindicação, enquanto o
     * lease é concedido com o {@code now()} do banco, necessariamente posterior. O desvio, portanto, joga a
     * favor: o prazo calculado aqui é sempre conservador em relação ao real.
     */
    public Instant deadlineFrom(Instant claimedAt) {
        requireNonNull(claimedAt, "Claimed at must not be null");
        return claimedAt.plus(lockDuration).minus(deliveryTimeout);
    }

    private static void requiredPositive(Duration value, String fieldName) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}
