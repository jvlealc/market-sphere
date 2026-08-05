package io.github.jvlealc.marketsphere.billing.infrastructure.config.props;

import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxRelaySettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "market-sphere.outbox")
@Validated
public record OutboxRelayProps(
        @NotNull @Valid Relay orderBilledMessaging,
        @NotNull @Valid Relay orderBilledEmail
) {

    public record Relay(
            @Positive int batchSize,
            @NotNull Duration lockDuration,
            @NotNull Duration deliveryTimeout,
            @NotNull Duration retryDelay,
            @NotNull Duration initialDelay,
            @NotNull Duration fixedDelay
    ) {

        /**
         * Converte para o modelo da aplicação — que é onde a invariante
         * {@code deliveryTimeout < lockDuration} é verificada. A cadência do agendador não atravessa: ela é
         * do adaptador de entrada, não do algoritmo de relay.
         */
        public OutboxRelaySettings toSettings() {
            return new OutboxRelaySettings(batchSize, lockDuration, deliveryTimeout, retryDelay);
        }
    }
}
