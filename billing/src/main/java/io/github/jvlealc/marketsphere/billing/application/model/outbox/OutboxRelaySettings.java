package io.github.jvlealc.marketsphere.billing.application.model.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Parâmetros de operação de um worker de relay da outbox.
 * <p>
 * Eles não são configurações independentes: existe uma relação entre {@code deliveryTimeout} e
 * {@code lockDuration} que, se violada, produz <strong>evento duplicado</strong>. Estarem num objeto só é o
 * que permite validá-la <em>no boot</em>, e não em produção.
 *
 * <h2>A invariante</h2>
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
        Objects.requireNonNull(lockDuration, "lockDuration must not be null");
        Objects.requireNonNull(deliveryTimeout, "deliveryTimeout must not be null");
        Objects.requireNonNull(retryDelay, "retryDelay must not be null");

        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }

        requirePositive(lockDuration, "lockDuration");
        requirePositive(deliveryTimeout, "deliveryTimeout");
        requirePositive(retryDelay, "retryDelay");

        if (deliveryTimeout.compareTo(lockDuration) >= 0) {
            throw new IllegalArgumentException(
                    "deliveryTimeout (%s) must be shorter than lockDuration (%s), otherwise the lease can expire while the message is still being delivered and another worker will publish it again"
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
        Objects.requireNonNull(claimedAt, "claimedAt must not be null");
        return claimedAt.plus(lockDuration).minus(deliveryTimeout);
    }

    private static void requirePositive(Duration value, String fieldName) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}
