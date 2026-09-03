package io.github.jvlealc.marketsphere.shipping.outbox;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Propriedades de relay
 *
 * @param batchSize       número máximo de mensagens reivindicadas por rodada
 * @param deliveryTimeout tempo máximo reservado para uma tentativa de entrega
 * @param claimDuration   prazo gravado em {@code next_attempt_at} ao reivindicar. Passado ele, a
 *                        linha volta a ser reivindicável — é assim que uma mensagem cujo worker
 *                        morreu no meio da publicação não fica presa em {@code PROCESSING}
 * @param retryDelay      intervalo antes da primeira retentativa
 * @param retryMultiplier fator de crescimento do intervalo a cada falha
 * @param retryMaxDelay   teto do intervalo. Dimensione-o pela duração da indisponibilidade que se
 *                        quer sobreviver: com teto baixo, {@code max_attempts} se esgota em minutos
 *                        e uma queda de horas manda tudo para {@code DEAD}
 */
@ConfigurationProperties(prefix = "market-sphere.outbox.relay")
@Validated
public record OutboxRelayProps(
        @Positive int batchSize,
        @NotNull Duration deliveryTimeout,
        @NotNull Duration claimDuration,
        @NotNull Duration retryDelay,
        @DecimalMin("1.0") double retryMultiplier,
        @NotNull Duration retryMaxDelay
) {

    public OutboxRelayProps {
        requirePositive(deliveryTimeout, "deliveryTimeout");
        requirePositive(claimDuration, "claimDuration");
        requirePositive(retryDelay, "retryDelay");
        requirePositive(retryMaxDelay, "retryMaxDelay");

        if (retryMaxDelay.compareTo(retryDelay) < 0) {
            throw new IllegalArgumentException(
                    "retryMaxDelay (%s) must not be shorter than retryDelay (%s)"
                            .formatted(retryMaxDelay, retryDelay));
        }

        Duration worstCaseBatch = deliveryTimeout.multipliedBy(batchSize);

        if (claimDuration.compareTo(worstCaseBatch) <= 0) {
            throw new IllegalArgumentException(
                    ("claimDuration (%s) must exceed batchSize x deliveryTimeout (%s). The deadline is granted to the "
                            + "whole batch at claim time but messages are delivered in series, so a shorter window "
                            + "lets another worker take back a row that is still being published, and the event goes out twice")
                            .formatted(claimDuration, worstCaseBatch));
        }
    }

    public Duration backoffFor(int attemptsAlreadyMade) {
        if (attemptsAlreadyMade <= 0) {
            return retryDelay;
        }

        double millis = retryDelay.toMillis() * Math.pow(retryMultiplier, attemptsAlreadyMade);

        return millis >= retryMaxDelay.toMillis()
                ? retryMaxDelay
                : Duration.ofMillis((long) millis);
    }

    private static void requirePositive(Duration value, String fieldName) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}
