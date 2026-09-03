package io.github.jvlealc.marketsphere.billing.application.model.outbox;

import io.github.jvlealc.marketsphere.billing.application.model.messaging.EventLineage;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxChannel.EMAIL;
import static io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxChannel.MESSAGING;
import static io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxStatus.DEAD;
import static io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * As invariantes desta classe são as do DDL: o que ela recusa em memória é o que o banco recusaria no
 * flush, com a diferença de que aqui o erro nomeia a regra em vez de devolver violação de constraint no
 * meio da transação de negócio.
 * <p>
 * As regras de chave, de prazo e de tentativa moram no construtor privado e valem igualmente para
 * {@code createNew} e {@code rehydrate}.
 */
class OutboxMessageTest {

    private static final UUID MESSAGE_ID = UUID.fromString("019ff81e-2e41-7c42-b1fa-2a239481cb3e");
    private static final String AGGREGATE_ID = "01a05a27-32af-7c42-9d0e-5f11c8a7b204";
    private static final String MESSAGE_KEY = "100";
    private static final String IDEMPOTENCY_KEY = "ORDER_BILLED:100:MESSAGING";
    private static final int EVENT_VERSION = 1;
    private static final int MAX_ATTEMPTS = 5;

    private static final Instant OCCURRED_AT     = Instant.parse("2026-09-01T10:00:00Z");
    private static final Instant NEXT_ATTEMPT_AT = Instant.parse("2026-09-01T10:00:15Z");

    private static final SerializedOutboxPayload PAYLOAD =
            new SerializedOutboxPayload("{\"orderId\":100,\"invoiceId\":\"01a05a27\"}");

    private static final EventLineage LINEAGE =
            new EventLineage("01a05a27-32af-7c42-b1fa-2a239481cb3e", null);

    private static final OutboxFailureReason FAILURE_REASON =
            OutboxFailureReason.of("Kafka: broker not available");

    // ------------------------------------------------------------------ createNew

    @Nested
    class CreateNew {

        @Test
        void shouldStartPending() {
            OutboxMessage message = newMessage();

            assertThat(message.getStatus()).isEqualTo(PENDING);
            assertThat(message.getAttempts()).isZero();
            assertThat(message.getMaxAttempts()).isEqualTo(MAX_ATTEMPTS);
            assertThat(message.getFailureReason()).isNull();
        }

        /** O id da linha é o {@code eventId} publicado no header, então a mensagem cunha o próprio. */
        @Test
        void shouldMintItsOwnId() {
            assertThat(newMessage().getId()).isNotNull();
            assertThat(newMessage().getId()).isNotEqualTo(newMessage().getId());
        }

        @Test
        void shouldScheduleFirstAttempt() {
            assertThat(newMessage().getNextAttemptAt()).isEqualTo(NEXT_ATTEMPT_AT);
        }

        @Test
        void shouldFreezeEnvelope() {
            OutboxMessage message = newMessage();

            assertThat(message.getAggregateType()).isEqualTo(OutboxAggregateType.INVOICE);
            assertThat(message.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(message.getEventType()).isEqualTo(OutboxEventType.ORDER_BILLED);
            assertThat(message.getEventVersion()).isEqualTo(EVENT_VERSION);
            assertThat(message.getOccurredAt()).isEqualTo(OCCURRED_AT);
            assertThat(message.getChannel()).isEqualTo(MESSAGING);
            assertThat(message.getMessageKey()).isEqualTo(MESSAGE_KEY);
            assertThat(message.getPayload()).isEqualTo(PAYLOAD);
            assertThat(message.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
            assertThat(message.getEventLineage()).isEqualTo(LINEAGE);
        }

        @Test
        void shouldNormalizeTextFields_whenTheyArriveWithSurroundingSpaces() {
            OutboxMessage message = OutboxMessage.createNew(
                    OutboxAggregateType.INVOICE,
                    "  " + AGGREGATE_ID + "  ",
                    OutboxEventType.ORDER_BILLED,
                    EVENT_VERSION,
                    OCCURRED_AT,
                    MESSAGING,
                    "  " + MESSAGE_KEY + "  ",
                    PAYLOAD,
                    "  " + IDEMPOTENCY_KEY + "  ",
                    LINEAGE,
                    NEXT_ATTEMPT_AT);

            assertThat(message.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(message.getMessageKey()).isEqualTo(MESSAGE_KEY);
            assertThat(message.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        }
    }

    // ------------------------------------------------------------------ rehydrate

    @Nested
    class Rehydrate {

        @Test
        void shouldRestoreStoredMessage() {
            OutboxMessage message = storedMessage(f -> { });

            assertThat(message.getId()).isEqualTo(MESSAGE_ID);
            assertThat(message.getStatus()).isEqualTo(PENDING);
            assertThat(message.getNextAttemptAt()).isEqualTo(NEXT_ATTEMPT_AT);
            assertThat(message.getAttempts()).isZero();
        }

        /**
         * Uma linha promovida a {@code DEAD} tem o prazo zerado pelo próprio UPDATE que a promove. Exigir
         * o campo aqui tornaria irreidratável exatamente a linha que se quer inspecionar depois.
         */
        @Test
        void shouldRestoreDeadMessage_whenItCarriesNoNextAttempt() {
            OutboxMessage message = storedMessage(f -> {
                f.status = DEAD;
                f.nextAttemptAt = null;
                f.attempts = MAX_ATTEMPTS;
                f.failureReason = FAILURE_REASON;
            });

            assertThat(message.getStatus()).isEqualTo(DEAD);
            assertThat(message.getNextAttemptAt()).isNull();
            // isSameAs, e não isEqualTo: OutboxFailureReason não implementa equals, então comparar por
            // valor aqui passaria por identidade sem dizer isso.
            assertThat(message.getFailureReason()).isSameAs(FAILURE_REASON);
        }

        @Test
        void shouldRestoreMessage_whenNoFailureReasonWasRecorded() {
            assertThat(storedMessage(f -> f.failureReason = null).getFailureReason()).isNull();
        }
    }

    // ------------------------------------------------------------ campos exigidos

    @Nested
    class RequiredFields {

        @ParameterizedTest(name = "{0}")
        @MethodSource("missingRequiredField")
        void shouldRejectMessage_whenRequiredFieldIsMissing(
                String label,
                Consumer<Fixture> override,
                Class<? extends RuntimeException> expectedType,
                String expectedMessage
        ) {
            assertThatThrownBy(rehydrationOf(override))
                    .isInstanceOf(expectedType)
                    .hasMessageContaining(expectedMessage);
        }

        static Stream<Arguments> missingRequiredField() {
            return Stream.of(
                    invalid("null ID", f -> f.id = null, NullPointerException.class, "id must not be null"),
                    invalid("null aggregate type", f -> f.aggregateType = null, NullPointerException.class, "aggregateType must not be null"),
                    invalid("null aggregate ID", f -> f.aggregateId = null, IllegalArgumentException.class, "aggregateId must not be null or blank"),
                    invalid("blank aggregate ID", f -> f.aggregateId = "   ", IllegalArgumentException.class, "aggregateId must not be null or blank"),
                    invalid("null event type", f -> f.eventType = null, NullPointerException.class, "eventType must not be null"),
                    invalid("null occurrence date", f -> f.occurredAt = null, NullPointerException.class, "occurredAt must not be null"),
                    invalid("null channel", f -> f.channel = null, NullPointerException.class, "channel must not be null"),
                    invalid("null payload", f -> f.payload = null, NullPointerException.class, "payload must not be null"),
                    invalid("null status", f -> f.status = null, NullPointerException.class, "status must not be null"),
                    invalid("null idempotency key", f -> f.idempotencyKey = null, IllegalArgumentException.class, "idempotencyKey must not be null or blank"),
                    invalid("blank idempotency key", f -> f.idempotencyKey = "   ", IllegalArgumentException.class, "idempotencyKey must not be null or blank"),
                    invalid("null event lineage", f -> f.eventLineage = null, NullPointerException.class, "eventLineage must not be null")
            );
        }

        /**
         * A versão existe desde a primeira mensagem para que nenhum consumidor precise tratar "ausente"
         * como v1 para sempre. Zero é ausência disfarçada de valor.
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void shouldRejectEventVersion_whenItIsNotPositive(int version) {
            assertThatThrownBy(rehydrationOf(f -> f.eventVersion = version))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eventVersion must be greater than zero");
        }
    }

    // ------------------------------------------------------------------ messageKey

    @Nested
    class MessageKey {

        /** A chave de partição é o {@code orderId}: sem ela, eventos do mesmo pedido se espalham. */
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectBlankMessageKey_whenChannelIsMessaging(String blankKey) {
            assertThatThrownBy(rehydrationOf(f -> {
                f.channel = MESSAGING;
                f.messageKey = blankKey;
            }))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("messageKey must not be null or blank");
        }

        /** No canal de e-mail não há partição a escolher, e o CHECK do banco espelha isso. */
        @Test
        void shouldRejectMessageKey_whenChannelIsEmail() {
            assertThatThrownBy(rehydrationOf(f -> {
                f.channel = EMAIL;
                f.messageKey = MESSAGE_KEY;
            }))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("messageKey must be null for the EMAIL channel");
        }

        @Test
        void shouldAcceptNoMessageKey_whenChannelIsEmail() {
            OutboxMessage message = storedMessage(f -> {
                f.channel = EMAIL;
                f.messageKey = null;
            });

            assertThat(message.getMessageKey()).isNull();
        }

        /**
         * Só o branco vira exceção no canal de e-mail: a string vazia é um valor, e o contrato ali é
         * ausência. É o mesmo rigor do CHECK, que compara com {@code null} e não com {@code ''}.
         */
        @Test
        void shouldRejectEmptyMessageKey_whenChannelIsEmail() {
            assertThatThrownBy(rehydrationOf(f -> {
                f.channel = EMAIL;
                f.messageKey = "";
            }))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("messageKey must be null for the EMAIL channel");
        }
    }

    // --------------------------------------------------------------- nextAttemptAt

    @Nested
    class NextAttemptAt {

        /** Status que ainda serão reivindicados precisam dizer a partir de quando. */
        @ParameterizedTest
        @EnumSource(value = OutboxStatus.class, names = {"PENDING", "FAILED"})
        void shouldRequireNextAttempt_whenStatusIsRetryable(OutboxStatus status) {
            assertThatThrownBy(rehydrationOf(f -> {
                f.status = status;
                f.nextAttemptAt = null;
            }))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("nextAttemptAt must not be null for status " + status);
        }

        /**
         * O oposto também é invariante, e é o que faltava no `orders`: um prazo gravado numa linha que
         * não será reivindicada de novo é estado impossível, não campo inofensivo.
         */
        @ParameterizedTest
        @EnumSource(value = OutboxStatus.class, names = {"PROCESSING", "PROCESSED", "DEAD"})
        void shouldRejectNextAttempt_whenStatusIsNotRetryable(OutboxStatus status) {
            assertThatThrownBy(rehydrationOf(f -> {
                f.status = status;
                f.nextAttemptAt = NEXT_ATTEMPT_AT;
            }))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nextAttemptAt must be null for status " + status);
        }

        @ParameterizedTest
        @EnumSource(value = OutboxStatus.class, names = {"PROCESSING", "PROCESSED", "DEAD"})
        void shouldAcceptNoNextAttempt_whenStatusIsNotRetryable(OutboxStatus status) {
            OutboxMessage message = storedMessage(f -> {
                f.status = status;
                f.nextAttemptAt = null;
            });

            assertThat(message.getNextAttemptAt()).isNull();
        }
    }

    // -------------------------------------------------------------------- attempts

    @Nested
    class Attempts {

        @Test
        void shouldRejectNegativeAttempts() {
            assertThatThrownBy(rehydrationOf(f -> f.attempts = -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("attempts must not be negative");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void shouldRejectMaxAttempts_whenItIsNotPositive(int maxAttempts) {
            assertThatThrownBy(rehydrationOf(f -> f.maxAttempts = maxAttempts))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxAttempts must be greater than zero");
        }

        @Test
        void shouldRejectAttempts_whenTheyExceedMaxAttempts() {
            assertThatThrownBy(rehydrationOf(f -> f.attempts = MAX_ATTEMPTS + 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("attempts must not be greater than maxAttempts");
        }

        /** A borda é `<=`, não `<`: a linha promovida a `DEAD` tem `attempts == maxAttempts`. */
        @Test
        void shouldAcceptAttempts_whenTheyEqualMaxAttempts() {
            OutboxMessage message = storedMessage(f -> {
                f.status = DEAD;
                f.nextAttemptAt = null;
                f.attempts = MAX_ATTEMPTS;
            });

            assertThat(message.getAttempts()).isEqualTo(MAX_ATTEMPTS);
        }

        /**
         * A coerência de tentativas é verificada antes de qualquer atribuição, então ela vence mesmo
         * quando outro campo também está errado. A ordem é deliberada e este teste a trava.
         */
        @Test
        void shouldValidateAttemptsBeforeAnyOtherField() {
            assertThatThrownBy(rehydrationOf(f -> {
                f.id = null;
                f.attempts = -1;
            }))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("attempts must not be negative");
        }
    }

    // -------------------------------------------------------------------- helpers

    private static OutboxMessage newMessage() {
        return OutboxMessage.createNew(
                OutboxAggregateType.INVOICE,
                AGGREGATE_ID,
                OutboxEventType.ORDER_BILLED,
                EVENT_VERSION,
                OCCURRED_AT,
                MESSAGING,
                MESSAGE_KEY,
                PAYLOAD,
                IDEMPOTENCY_KEY,
                LINEAGE,
                NEXT_ATTEMPT_AT);
    }

    private static OutboxMessage storedMessage(Consumer<Fixture> override) {
        Fixture fixture = new Fixture();
        override.accept(fixture);

        return fixture.build();
    }

    private static ThrowingCallable rehydrationOf(Consumer<Fixture> override) {
        return () -> storedMessage(override);
    }

    private static Arguments invalid(
            String label,
            Consumer<Fixture> override,
            Class<? extends RuntimeException> expectedType,
            String expectedMessage
    ) {
        return arguments(label, override, expectedType, expectedMessage);
    }

    /**
     * São dezesseis parâmetros posicionais: uma tabela com todos eles esconderia qual campo cada linha
     * está exercitando. O fixture nomeia só o que muda.
     */
    private static final class Fixture {

        UUID id = MESSAGE_ID;
        OutboxAggregateType aggregateType = OutboxAggregateType.INVOICE;
        String aggregateId = AGGREGATE_ID;
        OutboxEventType eventType = OutboxEventType.ORDER_BILLED;
        int eventVersion = EVENT_VERSION;
        Instant occurredAt = OCCURRED_AT;
        OutboxChannel channel = MESSAGING;
        String messageKey = MESSAGE_KEY;
        SerializedOutboxPayload payload = PAYLOAD;
        OutboxStatus status = PENDING;
        int attempts = 0;
        int maxAttempts = MAX_ATTEMPTS;
        Instant nextAttemptAt = NEXT_ATTEMPT_AT;
        String idempotencyKey = IDEMPOTENCY_KEY;
        EventLineage eventLineage = LINEAGE;
        OutboxFailureReason failureReason = null;

        OutboxMessage build() {
            return OutboxMessage.rehydrate(
                    id, aggregateType, aggregateId, eventType, eventVersion, occurredAt,
                    channel, messageKey, payload, status, attempts, maxAttempts,
                    nextAttemptAt, idempotencyKey, eventLineage, failureReason);
        }
    }
}
