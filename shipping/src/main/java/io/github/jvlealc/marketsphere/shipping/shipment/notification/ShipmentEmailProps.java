package io.github.jvlealc.marketsphere.shipping.shipment.notification;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Politica de retentativa do e-mail, deliberadamente muito mais espacada que a do relay de eventos:
 * a janela util de uma notificacao ao cliente e de horas a dias, e ele tem o site enquanto isso.
 *
 * @param maxAttempts teto de tentativas. Esgotado, a remessa para de ser recolhida pela varredura e
 *                    passa a ser caso operacional — esgotamento silencioso e o defeito a evitar
 */
@ConfigurationProperties(prefix = "market-sphere.shipment.email")
@Validated
public record ShipmentEmailProps(
        @Positive int batchSize,
        @Positive int maxAttempts,
        @NotNull Duration retryDelay,
        @DecimalMin("1.0") double retryMultiplier,
        @NotNull Duration retryMaxDelay
) {

    public Duration backoffFor(int attemptsAlreadyMade) {
        if (attemptsAlreadyMade <= 0) {
            return retryDelay;
        }

        double seconds = retryDelay.toSeconds() * Math.pow(retryMultiplier, attemptsAlreadyMade);

        return seconds >= retryMaxDelay.toSeconds()
                ? retryMaxDelay
                : Duration.ofSeconds((long) seconds);
    }
}
