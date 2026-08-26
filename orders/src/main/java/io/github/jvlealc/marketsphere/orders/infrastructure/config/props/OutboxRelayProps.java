package io.github.jvlealc.marketsphere.orders.infrastructure.config.props;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxRelaySettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "market-sphere.outbox")
@Validated
public record OutboxRelayProps(
        @NotNull @Valid Relay paymentRequest,
        @NotNull @Valid Relay orderPaidMessaging,
        @NotNull @Valid Relay orderPaidEmail,
        @NotNull @Valid Relay orderReadyForShipment
) {

    /**
     * Parâmetros de operação de um worker de relay.
     *
     * <p>Eles não são independentes: derivam uns dos outros, nesta ordem — os timeouts do transporte
     * definem o {@code deliveryTimeout}, que define o {@code lockDuration}, que define o
     * {@code batchSize}. Escolher o {@code lockDuration} primeiro, por parecer razoável, é a origem
     * mais comum de configuração incoerente.
     *
     * @param batchSize quantas mensagens o worker reivindica por rodada. É botão de <em>vazão</em>, não de
     *                  correção: como o relay interrompe o lote ao se aproximar do fim do lease, um lote
     *                  grande demais apenas devolve o excedente para a rodada seguinte. Dimensione por
     *                  {@code (lockDuration - deliveryTimeout) / tempo típico de uma entrega} — o tempo
     *                  típico, não o timeout.
     * @param lockDuration por quanto tempo a mensagem fica reservada para este worker. Tem dois custos
     *                     opostos: curto demais deixa o lease expirar durante a entrega e produz evento
     *                     duplicado; longo demais prende a mensagem por todo esse tempo depois de uma queda
     *                     do processo, já que só então outro worker pode reivindicá-la. Regra prática:
     *                     {@code max(2 × deliveryTimeout, latência de recuperação tolerável)}.
     * @param deliveryTimeout orçamento máximo de <strong>uma</strong> entrega. Não se escolhe: soma-se a
     *                        partir dos timeouts do próprio transporte — no Kafka, o
     *                        {@code delivery.timeout.ms} do produtor; no SMTP,
     *                        {@code connectiontimeout + timeout + writetimeout}. Precisa ser
     *                        <em>maior ou igual</em> ao orçamento do transporte: se o worker desistir antes
     *                        dele, registra falha e reagenda enquanto a entrega original ainda pode
     *                        concluir, duplicando o evento.
     * @param retryDelay espera antes da próxima tentativa de uma mensagem que falhou. Dimensione pela
     *                   duração da <em>indisponibilidade</em> que se quer sobreviver, não pela duração da
     *                   falha: as tentativas são consumidas por manutenção de rotina, e
     *                   {@code maxAttempts × (deliveryTimeout + retryDelay + fixedDelay/2)} precisa passar
     *                   da janela de indisponibilidade mais longa esperada, sob pena de mensagens sadias
     *                   virarem {@code DEAD} por causa de um restart de broker.
     * @param fixedDelay intervalo entre rodadas do agendador. Define o piso de latência — uma mensagem
     *                   recém-gravada espera em média {@code fixedDelay / 2} até ser vista. Não atravessa
     *                   para {@link OutboxRelaySettings}: é cadência do adaptador de entrada, não do
     *                   algoritmo de relay.
     */
    public record Relay(
            @Positive int batchSize,
            @NotNull Duration lockDuration,
            @NotNull Duration deliveryTimeout,
            @NotNull Duration retryDelay,
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
