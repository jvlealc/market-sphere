package io.github.jvlealc.marketsphere.shipping.outbox;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * As invariantes deste record são a única defesa contra duas configurações que não quebram nada no
 * arranque e sim em produção: prazo de reivindicação curto demais, que faz o evento sair duas vezes, e
 * teto de backoff curto demais, que manda a mensagem para {@code DEAD} antes de a indisponibilidade
 * passar.
 * <p>
 * As anotações de Bean Validation ({@code @Positive}, {@code @DecimalMin}) agem na ligação das
 * propriedades, não na construção. Por isso não são testados.
 */
class OutboxRelayPropsTest {

    private static final int BATCH_SIZE = 10;
    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration CLAIM_DURATION = Duration.ofSeconds(30);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(10);
    private static final double RETRY_MULTIPLIER = 2.0;
    private static final Duration RETRY_MAX_DELAY = Duration.ofMinutes(1);

    // ------------------------------------------------------------- invariantes

    @Nested
    class Invariants {

        @Test
        void shouldAcceptCoherentSettings() {
            OutboxRelayProps props = props();

            assertThat(props.batchSize()).isEqualTo(BATCH_SIZE);
            assertThat(props.claimDuration()).isEqualTo(CLAIM_DURATION);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("io.github.jvlealc.marketsphere.shipping.outbox.OutboxRelayPropsTest#nonPositiveDurations")
        void shouldRejectDuration_whenItIsNotPositive(String field, ThrowingCallable creation) {
            assertThatThrownBy(creation)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(field + " must be greater than zero");
        }

        @Test
        void shouldRejectRetryMaxDelay_whenItIsShorterThanRetryDelay() {
            assertThatThrownBy(() -> props(
                    BATCH_SIZE, DELIVERY_TIMEOUT, CLAIM_DURATION,
                    Duration.ofSeconds(30), RETRY_MULTIPLIER, Duration.ofSeconds(29)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("retryMaxDelay");
        }

        /**
         * O prazo é concedido ao lote inteiro no momento da reivindicação, mas as mensagens são entregues
         * em série. Se ele couber no pior caso do lote, outro worker retoma uma linha que ainda está sendo
         * publicada, e o evento sai duas vezes.
         */
        @Test
        void shouldRejectClaimDuration_whenItDoesNotExceedWorstCaseBatch() {
            Duration worstCase = DELIVERY_TIMEOUT.multipliedBy(BATCH_SIZE);

            assertThatThrownBy(() -> props(
                    BATCH_SIZE, DELIVERY_TIMEOUT, worstCase.minusSeconds(1),
                    RETRY_DELAY, RETRY_MULTIPLIER, RETRY_MAX_DELAY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("claimDuration");
        }

        @Test
        void shouldRejectClaimDuration_whenItOnlyEqualsWorstCaseBatch() {
            Duration worstCase = DELIVERY_TIMEOUT.multipliedBy(BATCH_SIZE);

            assertThatThrownBy(() -> props(
                    BATCH_SIZE, DELIVERY_TIMEOUT, worstCase,
                    RETRY_DELAY, RETRY_MULTIPLIER, RETRY_MAX_DELAY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("claimDuration");
        }
    }

    // ---------------------------------------------------------------- backoff

    @Nested
    class BackoffFor {

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void shouldUseConfiguredDelay_whenNoAttemptWasMade(int attemptsAlreadyMade) {
            assertThat(props().backoffFor(attemptsAlreadyMade)).isEqualTo(RETRY_DELAY);
        }

        @Test
        void shouldGrowByMultiplierOnEachAttempt() {
            OutboxRelayProps props = props();

            assertThat(props.backoffFor(1)).isEqualTo(Duration.ofSeconds(20));
            assertThat(props.backoffFor(2)).isEqualTo(Duration.ofSeconds(40));
        }

        @Test
        void shouldCapAtConfiguredMaximum() {
            assertThat(props().backoffFor(3)).isEqualTo(RETRY_MAX_DELAY);
        }

        /** Sem o teto, o {@code Math.pow} estoura para infinito antes de virar {@link Duration}. */
        @Test
        void shouldCapAtConfiguredMaximum_whenGrowthOverflows() {
            assertThat(props().backoffFor(2_000)).isEqualTo(RETRY_MAX_DELAY);
        }

        /**
         * Delay abaixo de um segundo passa pelas invariantes, então o cálculo não pode truncá-lo: em
         * segundos inteiros ele viraria zero, e a mensagem gastaria as dez tentativas sem espera nenhuma.
         */
        @Test
        void shouldPreserveSubSecondDelays() {
            OutboxRelayProps props = props(
                    BATCH_SIZE, DELIVERY_TIMEOUT, CLAIM_DURATION,
                    Duration.ofMillis(500), RETRY_MULTIPLIER, RETRY_MAX_DELAY);

            assertThat(props.backoffFor(1)).isEqualTo(Duration.ofSeconds(1));
        }
    }

    // ---------------------------------------------------------------- fixtures

    static Stream<Arguments> nonPositiveDurations() {
        return Stream.of(
                arguments("deliveryTimeout", (ThrowingCallable) () -> props(
                        BATCH_SIZE, Duration.ZERO, CLAIM_DURATION, RETRY_DELAY, RETRY_MULTIPLIER, RETRY_MAX_DELAY)),
                arguments("deliveryTimeout", (ThrowingCallable) () -> props(
                        BATCH_SIZE, Duration.ofSeconds(-1), CLAIM_DURATION, RETRY_DELAY, RETRY_MULTIPLIER, RETRY_MAX_DELAY)),
                arguments("claimDuration", (ThrowingCallable) () -> props(
                        BATCH_SIZE, DELIVERY_TIMEOUT, Duration.ZERO, RETRY_DELAY, RETRY_MULTIPLIER, RETRY_MAX_DELAY)),
                arguments("claimDuration", (ThrowingCallable) () -> props(
                        BATCH_SIZE, DELIVERY_TIMEOUT, Duration.ofSeconds(-1), RETRY_DELAY, RETRY_MULTIPLIER, RETRY_MAX_DELAY)),
                arguments("retryDelay", (ThrowingCallable) () -> props(
                        BATCH_SIZE, DELIVERY_TIMEOUT, CLAIM_DURATION, Duration.ZERO, RETRY_MULTIPLIER, RETRY_MAX_DELAY)),
                arguments("retryDelay", (ThrowingCallable) () -> props(
                        BATCH_SIZE, DELIVERY_TIMEOUT, CLAIM_DURATION, Duration.ofSeconds(-1), RETRY_MULTIPLIER, RETRY_MAX_DELAY)),
                arguments("retryMaxDelay", (ThrowingCallable) () -> props(
                        BATCH_SIZE, DELIVERY_TIMEOUT, CLAIM_DURATION, RETRY_DELAY, RETRY_MULTIPLIER, Duration.ZERO)),
                arguments("retryMaxDelay", (ThrowingCallable) () -> props(
                        BATCH_SIZE, DELIVERY_TIMEOUT, CLAIM_DURATION, RETRY_DELAY, RETRY_MULTIPLIER, Duration.ofSeconds(-1)))
        );
    }

    private static OutboxRelayProps props() {
        return props(BATCH_SIZE, DELIVERY_TIMEOUT, CLAIM_DURATION, RETRY_DELAY, RETRY_MULTIPLIER, RETRY_MAX_DELAY);
    }

    private static OutboxRelayProps props(
            int batchSize,
            Duration deliveryTimeout,
            Duration claimDuration,
            Duration retryDelay,
            double retryMultiplier,
            Duration retryMaxDelay
    ) {
        return new OutboxRelayProps(
                batchSize, deliveryTimeout, claimDuration, retryDelay, retryMultiplier, retryMaxDelay);
    }
}
