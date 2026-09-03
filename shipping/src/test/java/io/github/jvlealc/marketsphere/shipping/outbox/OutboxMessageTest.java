package io.github.jvlealc.marketsphere.shipping.outbox;

import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A linha da outbox nasce sempre {@code PENDING}, com zero tentativas e sem desfecho: as transições
 * para {@code PROCESSING}, {@code PROCESSED}, {@code FAILED} e {@code DEAD} são {@code UPDATE} nativos
 * do repositório, e nenhuma passa por aqui. O que este teste cobre, portanto, é a construção e o
 * estado inicial, que é o contrato com a consulta de reivindicação.
 */
class OutboxMessageTest {

    private static final int MAX_ATTEMPTS = 10;

    private static final String AGGREGATE_ID = "018f4b0e-6f2a-7c31-9a44-5c1d2e3f4a5b";
    private static final int EVENT_VERSION = 1;
    private static final String MESSAGE_KEY = "100";
    private static final String IDEMPOTENCY_KEY = "order-shipped-shipment-018f4b0e-6f2a-7c31-9a44-5c1d2e3f4a5b";

    private static final String CORRELATION_ID = "01a05a27-32af-7c42-b1fa-2a239481cb3e";
    private static final String CAUSATION_ID = "01a05a27-4c9e-7d18-8b02-9f7a1c6d5e40";

    private static final Instant OCCURRED_AT = Instant.parse("2026-09-01T10:00:00Z");
    private static final Instant NEXT_ATTEMPT_AT = Instant.parse("2026-09-01T10:00:05Z");

    private static final OutboxEventType EVENT_TYPE = OutboxEventType.ORDER_SHIPPED;
    private static final SerializedOutboxPayload PAYLOAD =
            new SerializedOutboxPayload("{\"orderId\":100,\"trackingCode\":\"BR-2ijs7Su29DaA5\"}");

    // ---------------------------------------------------------------- createNew

    @Nested
    class CreateNew {

        @Test
        void shouldStartPendingWithNoAttemptAndNoOutcome() {
            OutboxMessage message = new Fixture().create();

            assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(message.getAttempts()).isZero();
            assertThat(message.getMaxAttempts()).isEqualTo(MAX_ATTEMPTS);
            assertThat(message.getProcessedAt()).isNull();
            assertThat(message.getFailureReason()).isNull();
        }

        /**
         * A identidade é cunhada aqui, e não pelo banco: a mensagem precisa dela para virar o
         * {@code eventId} do header antes de qualquer {@code insert}.
         */
        @Test
        void shouldMintItsOwnUuidV7Id() {
            OutboxMessage message = new Fixture().create();

            assertThat(message.getId()).isNotNull();
            assertThat(message.getId().version()).isEqualTo(7);
            assertThat(message.getId()).isNotEqualTo(new Fixture().create().getId());
        }

        @Test
        void shouldBeNewUntilItReachesDatabase() {
            assertThat(new Fixture().create().isNew()).isTrue();
        }

        @Test
        void shouldFreezeEnvelope() {
            OutboxMessage message = new Fixture().create();

            assertThat(message.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(message.getEventType()).isEqualTo(EVENT_TYPE);
            assertThat(message.getEventVersion()).isEqualTo(EVENT_VERSION);
            assertThat(message.getOccurredAt()).isEqualTo(OCCURRED_AT);
            assertThat(message.getMessageKey()).isEqualTo(MESSAGE_KEY);
            assertThat(message.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
            assertThat(message.getPayload()).isEqualTo(PAYLOAD);
            assertThat(message.getEventLineage()).isEqualTo(new EventLineage(CORRELATION_ID, CAUSATION_ID));
        }

        @Test
        void shouldScheduleFirstAttempt() {
            assertThat(new Fixture().create().getNextAttemptAt()).isEqualTo(NEXT_ATTEMPT_AT);
        }

        @Test
        void shouldNormalizeTextFields_whenTheyArriveWithSurroundingSpaces() {
            OutboxMessage message = new Fixture()
                    .withAggregateId("  " + AGGREGATE_ID + "  ")
                    .withMessageKey("  " + MESSAGE_KEY + "  ")
                    .withIdempotencyKey("  " + IDEMPOTENCY_KEY + "  ")
                    .create();

            assertThat(message.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(message.getMessageKey()).isEqualTo(MESSAGE_KEY);
            assertThat(message.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        }

        /** {@code causationId} nulo identifica a raiz de um fluxo, não uma ausência de dado. */
        @Test
        void shouldAcceptNoCausationId_whenEventStartsFlow() {
            OutboxMessage message = new Fixture()
                    .withLineage(new EventLineage(CORRELATION_ID, null))
                    .create();

            assertThat(message.getEventLineage().causationId()).isNull();
            assertThat(message.getEventLineage().correlationId()).isEqualTo(CORRELATION_ID);
        }
    }

    // ----------------------------------------------------------- campos exigidos

    @Nested
    class RequiredFields {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectAggregateId_whenItIsBlank(String blank) {
            assertThatThrownBy(() -> new Fixture().withAggregateId(blank).create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("aggregateId must not be null or blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectMessageKey_whenItIsBlank(String blank) {
            assertThatThrownBy(() -> new Fixture().withMessageKey(blank).create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("messageKey must not be null or blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectIdempotencyKey_whenItIsBlank(String blank) {
            assertThatThrownBy(() -> new Fixture().withIdempotencyKey(blank).create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("idempotencyKey must not be null or blank");
        }

        @Test
        void shouldRejectMissingEventType() {
            assertThatThrownBy(() -> new Fixture().withEventType(null).create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("eventType must not be null");
        }

        @Test
        void shouldRejectMissingOccurrenceDate() {
            assertThatThrownBy(() -> new Fixture().withOccurredAt(null).create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("occurredAt must not be null");
        }

        @Test
        void shouldRejectMissingPayload() {
            assertThatThrownBy(() -> new Fixture().withPayload(null).create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("payload must not be null");
        }

        @Test
        void shouldRejectMissingLineage() {
            assertThatThrownBy(() -> new Fixture().withLineage(null).create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("lineage must not be null");
        }

        /**
         * Sem data de próxima tentativa a linha nasce fora do alcance da consulta de reivindicação, ela
         * exige {@code next_attempt_at <= now()}.
         */
        @Test
        void shouldRejectMissingNextAttemptDate() {
            assertThatThrownBy(() -> new Fixture().withNextAttemptAt(null).create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("nextAttemptAt must not be null");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void shouldRejectEventVersion_whenItIsNotPositive(int eventVersion) {
            assertThatThrownBy(() -> new Fixture().withEventVersion(eventVersion).create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("eventVersion must be greater than zero");
        }
    }

    // ---------------------------------------------------------------- fixtures

    private static final class Fixture {

        private String aggregateId = AGGREGATE_ID;
        private OutboxEventType eventType = EVENT_TYPE;
        private int eventVersion = EVENT_VERSION;
        private Instant occurredAt = OCCURRED_AT;
        private String messageKey = MESSAGE_KEY;
        private EventLineage lineage = new EventLineage(CORRELATION_ID, CAUSATION_ID);
        private SerializedOutboxPayload payload = PAYLOAD;
        private Instant nextAttemptAt = NEXT_ATTEMPT_AT;
        private String idempotencyKey = IDEMPOTENCY_KEY;

        private Fixture withAggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        private Fixture withEventType(OutboxEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        private Fixture withEventVersion(int eventVersion) {
            this.eventVersion = eventVersion;
            return this;
        }

        private Fixture withOccurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        private Fixture withMessageKey(String messageKey) {
            this.messageKey = messageKey;
            return this;
        }

        private Fixture withLineage(EventLineage lineage) {
            this.lineage = lineage;
            return this;
        }

        private Fixture withPayload(SerializedOutboxPayload payload) {
            this.payload = payload;
            return this;
        }

        private Fixture withNextAttemptAt(Instant nextAttemptAt) {
            this.nextAttemptAt = nextAttemptAt;
            return this;
        }

        private Fixture withIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        private OutboxMessage create() {
            return OutboxMessage.createNew(
                    aggregateId,
                    eventType,
                    eventVersion,
                    occurredAt,
                    messageKey,
                    lineage,
                    payload,
                    nextAttemptAt,
                    idempotencyKey
            );
        }
    }
}
