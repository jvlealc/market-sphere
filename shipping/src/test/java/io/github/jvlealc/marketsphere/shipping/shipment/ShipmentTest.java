package io.github.jvlealc.marketsphere.shipping.shipment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.stream.Stream;

import static io.github.jvlealc.marketsphere.shipping.shipment.ShipmentStatus.CANCELED;
import static io.github.jvlealc.marketsphere.shipping.shipment.ShipmentStatus.PREPARING_SHIPMENT;
import static io.github.jvlealc.marketsphere.shipping.shipment.ShipmentStatus.SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ShipmentTest {

    private static final long ORDER_ID = 100L;
    private static final long CUSTOMER_ID = 1L;

    private static final Instant BILLED_AT = Instant.parse("2026-09-01T09:00:00Z");
    private static final Instant SHIPPED_AT = Instant.parse("2026-09-01T10:00:00Z");
    private static final Instant OTHER_SHIPPED_AT = Instant.parse("2026-09-01T10:30:00Z");
    private static final Instant CANCELED_AT = Instant.parse("2026-09-01T11:00:00Z");
    private static final Instant OTHER_CANCELED_AT = Instant.parse("2026-09-01T11:30:00Z");

    private static final Instant EMAIL_SENT_AT = Instant.parse("2026-09-01T12:00:00Z");
    private static final Instant OTHER_EMAIL_SENT_AT = Instant.parse("2026-09-01T12:30:00Z");
    private static final Instant NEXT_ATTEMPT_AT = Instant.parse("2026-09-01T13:00:00Z");
    private static final Instant LATER_ATTEMPT_AT = Instant.parse("2026-09-01T14:00:00Z");

    private static final String CUSTOMER_EMAIL = "customer@marketsphere.io";
    private static final String CUSTOMER_NAME = "Maria Silva";
    private static final String CORRELATION_ID = "01a05a27-32af-7c42-b1fa-2a239481cb3e";

    private static final String TRACKING_CODE = "BR-2ijs7Su29DaA5";
    private static final String CARRIER = "Correios";
    private static final String OTHER_TRACKING_CODE = "BR-9zXq1Ab44CdE2";
    private static final String OTHER_CARRIER = "Jadlog";

    // ------------------------------------------------- createPreparingShipment

    @Nested
    class CreatePreparingShipment {

        @Test
        void shouldStartPreparingShipment() {
            Shipment shipment = preparingShipment();

            assertThat(shipment.getStatus()).isEqualTo(PREPARING_SHIPMENT);
            assertThat(shipment.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(shipment.getBilledAt()).isEqualTo(BILLED_AT);
            assertThat(shipment.getId()).isNull();
        }

        @Test
        void shouldFreezeCustomerData() {
            Shipment shipment = preparingShipment();

            assertThat(shipment.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(shipment.getCustomerEmail()).isEqualTo(CUSTOMER_EMAIL);
            assertThat(shipment.getCustomerName()).isEqualTo(CUSTOMER_NAME);
        }

        /** A correlação é congelada na criação porque a borda REST do despacho não tem header de onde herdá-la. */
        @Test
        void shouldFreezeCorrelation() {
            Shipment shipment = preparingShipment();

            assertThat(shipment.getCorrelationId()).isEqualTo(CORRELATION_ID);
        }

        @Test
        void shouldCarryNoShippingData() {
            Shipment shipment = preparingShipment();

            assertThat(shipment.getShippedAt()).isNull();
            assertThat(shipment.getTrackingCode()).isNull();
            assertThat(shipment.getCarrier()).isNull();
            assertThat(shipment.getCanceledAt()).isNull();
        }

        @Test
        void shouldCarryNoEmailDeliveryHistory() {
            Shipment shipment = preparingShipment();

            assertThat(shipment.getShipmentEmailSentAt()).isNull();
            assertThat(shipment.getShipmentEmailAttempts()).isZero();
            assertThat(shipment.getShipmentEmailNextAttemptAt()).isNull();
        }

        @Test
        void shouldStampSameInstantOnCreationAndUpdate() {
            Shipment shipment = preparingShipment();

            assertThat(shipment.getCreatedAt()).isNotNull();
            assertThat(shipment.getUpdatedAt()).isEqualTo(shipment.getCreatedAt());
        }

        @Test
        void shouldNormalizeTextFields_whenTheyArriveWithSurroundingSpaces() {
            Shipment shipment = Shipment.createPreparingShipment(
                    ORDER_ID, BILLED_AT, CUSTOMER_ID,
                    "  " + CUSTOMER_EMAIL + "  ",
                    "  " + CUSTOMER_NAME + "  ",
                    "  " + CORRELATION_ID + "  ");

            assertThat(shipment.getCustomerEmail()).isEqualTo(CUSTOMER_EMAIL);
            assertThat(shipment.getCustomerName()).isEqualTo(CUSTOMER_NAME);
            assertThat(shipment.getCorrelationId()).isEqualTo(CORRELATION_ID);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidCreationData")
        void shouldRejectShipment_whenCreationDataIsInvalid(
                String label,
                Long orderId,
                Instant billedAt,
                Long customerId,
                String customerEmail,
                String customerName,
                String correlationId,
                String expectedMessage
        ) {
            assertThatThrownBy(() -> Shipment.createPreparingShipment(
                    orderId, billedAt, customerId, customerEmail, customerName, correlationId))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessage(expectedMessage);
        }

        static Stream<Arguments> invalidCreationData() {
            return Stream.of(
                    arguments("null order ID", null, BILLED_AT, CUSTOMER_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, CORRELATION_ID, "orderId must not be null"),
                    arguments("order ID zero", 0L, BILLED_AT, CUSTOMER_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, CORRELATION_ID, "orderId must be greater than zero"),
                    arguments("negative order ID", -1L, BILLED_AT, CUSTOMER_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, CORRELATION_ID, "orderId must be greater than zero"),
                    arguments("null customer ID", ORDER_ID, BILLED_AT, null, CUSTOMER_EMAIL, CUSTOMER_NAME, CORRELATION_ID, "customerId must not be null"),
                    arguments("customer ID zero", ORDER_ID, BILLED_AT, 0L, CUSTOMER_EMAIL, CUSTOMER_NAME, CORRELATION_ID, "customerId must be greater than zero"),
                    arguments("negative customer ID", ORDER_ID, BILLED_AT, -1L, CUSTOMER_EMAIL, CUSTOMER_NAME, CORRELATION_ID, "customerId must be greater than zero"),
                    arguments("null billed date", ORDER_ID, null, CUSTOMER_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, CORRELATION_ID, "billedAt must not be null"),
                    arguments("null customer email", ORDER_ID, BILLED_AT, CUSTOMER_ID, null, CUSTOMER_NAME, CORRELATION_ID, "customerEmail must not be null or blank"),
                    arguments("blank customer email", ORDER_ID, BILLED_AT, CUSTOMER_ID, "   ", CUSTOMER_NAME, CORRELATION_ID, "customerEmail must not be null or blank"),
                    arguments("null customer name", ORDER_ID, BILLED_AT, CUSTOMER_ID, CUSTOMER_EMAIL, null, CORRELATION_ID, "customerName must not be null or blank"),
                    arguments("blank customer name", ORDER_ID, BILLED_AT, CUSTOMER_ID, CUSTOMER_EMAIL, "   ", CORRELATION_ID, "customerName must not be null or blank"),
                    arguments("null correlation ID", ORDER_ID, BILLED_AT, CUSTOMER_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, null, "correlationId must not be null or blank"),
                    arguments("blank correlation ID", ORDER_ID, BILLED_AT, CUSTOMER_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, "   ", "correlationId must not be null or blank")
            );
        }
    }

    // ------------------------------------------------------------ markAsShipped

    @Nested
    class MarkAsShipped {

        @Test
        void shouldDispatchShipment() {
            Shipment shipment = preparingShipment();

            assertThat(shipment.markAsShipped(TRACKING_CODE, CARRIER, SHIPPED_AT)).isTrue();
            assertThat(shipment.getStatus()).isEqualTo(SHIPPED);
            assertThat(shipment.getTrackingCode()).isEqualTo(TRACKING_CODE);
            assertThat(shipment.getCarrier()).isEqualTo(CARRIER);
            assertThat(shipment.getShippedAt()).isEqualTo(SHIPPED_AT);
        }

        @Test
        void shouldNormalizeShippingData_whenItArrivesWithSurroundingSpaces() {
            Shipment shipment = preparingShipment();

            shipment.markAsShipped("  " + TRACKING_CODE + "  ", "  " + CARRIER + "  ", SHIPPED_AT);

            assertThat(shipment.getTrackingCode()).isEqualTo(TRACKING_CODE);
            assertThat(shipment.getCarrier()).isEqualTo(CARRIER);
        }

        @Test
        void shouldReportNoChange_whenSameShippingDataArrivesAgain() {
            Shipment shipment = shippedShipment();

            assertThat(shipment.markAsShipped(TRACKING_CODE, CARRIER, SHIPPED_AT)).isFalse();
            assertThat(shipment.getShippedAt()).isEqualTo(SHIPPED_AT);
        }

        /** Normalizar antes da guarda de idempotência é o que faz a reentrega com espaços continuar sendo no-op. */
        @Test
        void shouldReportNoChange_whenSameShippingDataArrivesWithSurroundingSpaces() {
            Shipment shipment = shippedShipment();

            assertThat(shipment.markAsShipped("  " + TRACKING_CODE + "  ", "  " + CARRIER + "  ", SHIPPED_AT)).isFalse();
        }

        @Test
        void shouldRejectDispatch_whenConflictingTrackingCodeArrives() {
            Shipment shipment = shippedShipment();

            assertThatThrownBy(() -> shipment.markAsShipped(OTHER_TRACKING_CODE, CARRIER, SHIPPED_AT))
                    .isInstanceOf(IllegalShipmentStatusChangeException.class)
                    .hasMessageContaining("Conflicting shipped data");

            assertThat(shipment.getTrackingCode()).isEqualTo(TRACKING_CODE);
        }

        @Test
        void shouldRejectDispatch_whenConflictingCarrierArrives() {
            Shipment shipment = shippedShipment();

            assertThatThrownBy(() -> shipment.markAsShipped(TRACKING_CODE, OTHER_CARRIER, SHIPPED_AT))
                    .isInstanceOf(IllegalShipmentStatusChangeException.class)
                    .hasMessageContaining("Conflicting shipped data");

            assertThat(shipment.getCarrier()).isEqualTo(CARRIER);
        }

        @Test
        void shouldRejectDispatch_whenShipmentIsCanceled() {
            Shipment shipment = canceledShipment();

            assertThatThrownBy(() -> shipment.markAsShipped(TRACKING_CODE, CARRIER, SHIPPED_AT))
                    .isInstanceOf(IllegalShipmentStatusChangeException.class)
                    .hasMessageContaining("Canceled shipment cannot be marked as shipped");

            assertThat(shipment.getStatus()).isEqualTo(CANCELED);
            assertThat(shipment.getTrackingCode()).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectDispatch_whenTrackingCodeIsBlank(String blankCode) {
            Shipment shipment = preparingShipment();

            assertThatThrownBy(() -> shipment.markAsShipped(blankCode, CARRIER, SHIPPED_AT))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessage("trackingCode must not be null or blank");

            assertThat(shipment.getStatus()).isEqualTo(PREPARING_SHIPMENT);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectDispatch_whenCarrierIsBlank(String blankCarrier) {
            Shipment shipment = preparingShipment();

            assertThatThrownBy(() -> shipment.markAsShipped(TRACKING_CODE, blankCarrier, SHIPPED_AT))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessage("carrier must not be null or blank");

            assertThat(shipment.getStatus()).isEqualTo(PREPARING_SHIPMENT);
        }

        /**
         * A ordem é deliberada: a entrada que a guarda de idempotência lê é normalizada antes dela,
         * então código em branco é recusado como argumento inválido mesmo depois do despacho.
         */
        @Test
        void shouldRejectDispatch_whenTrackingCodeIsBlankAfterShipping() {
            Shipment shipment = shippedShipment();

            assertThatThrownBy(() -> shipment.markAsShipped("   ", CARRIER, SHIPPED_AT))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessage("trackingCode must not be null or blank");
        }

        @Test
        void shouldKeepPreparingShipmentState_whenShippedDateIsMissing() {
            Shipment shipment = preparingShipment();

            assertThatThrownBy(() -> shipment.markAsShipped(TRACKING_CODE, CARRIER, null))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessage("shippedAt must not be null");

            assertThat(shipment.getStatus()).isEqualTo(PREPARING_SHIPMENT);
            assertThat(shipment.getTrackingCode()).isNull();
            assertThat(shipment.getCarrier()).isNull();
        }

        /**
         * A data não identifica o despacho, {@code trackingCode} e {@code carrier} identificam. O serviço
         * gera {@code shippedAt} quando o cliente não o envia, então compará-la transformaria uma
         * retentativa HTTP legítima em conflito.
         */
        @Test
        void shouldReportNoChange_whenOnlyShippedDateDiverges() {
            Shipment shipment = shippedShipment();

            assertThat(shipment.markAsShipped(TRACKING_CODE, CARRIER, OTHER_SHIPPED_AT)).isFalse();
            assertThat(shipment.getShippedAt()).isEqualTo(SHIPPED_AT);
        }
    }

    // ----------------------------------------------------------- markAsCanceled

    @Nested
    class MarkAsCanceled {

        @Test
        void shouldCancelShipment() {
            Shipment shipment = preparingShipment();

            assertThat(shipment.markAsCanceled(CANCELED_AT)).isTrue();
            assertThat(shipment.getStatus()).isEqualTo(CANCELED);
            assertThat(shipment.getCanceledAt()).isEqualTo(CANCELED_AT);
        }

        /** Espelha o {@code chk_shipments_canceled_data}: um cancelado nunca carrega dados de envio. */
        @Test
        void shouldLeaveShippingDataEmpty() {
            Shipment shipment = canceledShipment();

            assertThat(shipment.getShippedAt()).isNull();
            assertThat(shipment.getTrackingCode()).isNull();
            assertThat(shipment.getCarrier()).isNull();
        }

        @Test
        void shouldReportNoChange_whenShipmentIsAlreadyCanceled() {
            Shipment shipment = canceledShipment();

            assertThat(shipment.markAsCanceled(OTHER_CANCELED_AT)).isFalse();
            assertThat(shipment.getCanceledAt()).isEqualTo(CANCELED_AT);
        }

        @Test
        void shouldRejectCancellation_whenShipmentHasAlreadyShipped() {
            Shipment shipment = shippedShipment();

            assertThatThrownBy(() -> shipment.markAsCanceled(CANCELED_AT))
                    .isInstanceOf(IllegalShipmentStatusChangeException.class)
                    .hasMessageContaining("Shipped shipment cannot be canceled");

            assertThat(shipment.getStatus()).isEqualTo(SHIPPED);
            assertThat(shipment.getCanceledAt()).isNull();
        }

        @Test
        void shouldKeepCurrentState_whenCanceledDateIsMissing() {
            Shipment shipment = preparingShipment();

            assertThatThrownBy(() -> shipment.markAsCanceled(null))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessage("canceledAt must not be null");

            assertThat(shipment.getStatus()).isEqualTo(PREPARING_SHIPMENT);
        }
    }

    // -------------------------------------------------- markShipmentEmailAsSent

    @Nested
    class MarkShipmentEmailAsSent {

        @Test
        void shouldRecordDelivery() {
            Shipment shipment = shippedShipment();

            assertThat(shipment.markShipmentEmailAsSent(EMAIL_SENT_AT)).isTrue();
            assertThat(shipment.getShipmentEmailSentAt()).isEqualTo(EMAIL_SENT_AT);
        }

        /** O marcador registra efeito confirmado: gravá-lo tira a remessa da fila da varredura. */
        @Test
        void shouldClearPendingRetry_whenFailureWasRegisteredBefore() {
            Shipment shipment = shippedShipment();
            shipment.registerEmailDeliveryFailure(NEXT_ATTEMPT_AT);

            shipment.markShipmentEmailAsSent(EMAIL_SENT_AT);

            assertThat(shipment.getShipmentEmailNextAttemptAt()).isNull();
        }

        /** As tentativas anteriores permanecem: são auditoria do que custou entregar, não estado da fila. */
        @Test
        void shouldKeepFailedAttempts_whenDeliverySucceeds() {
            Shipment shipment = shippedShipment();
            shipment.registerEmailDeliveryFailure(NEXT_ATTEMPT_AT);
            shipment.registerEmailDeliveryFailure(LATER_ATTEMPT_AT);

            shipment.markShipmentEmailAsSent(EMAIL_SENT_AT);

            assertThat(shipment.getShipmentEmailAttempts()).isEqualTo(2);
        }

        @Test
        void shouldReportNoChange_whenDeliveryWasAlreadyRecorded() {
            Shipment shipment = shippedShipment();
            shipment.markShipmentEmailAsSent(EMAIL_SENT_AT);

            assertThat(shipment.markShipmentEmailAsSent(OTHER_EMAIL_SENT_AT)).isFalse();
            assertThat(shipment.getShipmentEmailSentAt()).isEqualTo(EMAIL_SENT_AT);
        }

        @Test
        void shouldRejectRecord_whenShipmentWasNotShipped() {
            Shipment shipment = preparingShipment();

            assertThatThrownBy(() -> shipment.markShipmentEmailAsSent(EMAIL_SENT_AT))
                    .isInstanceOf(IllegalShipmentStatusChangeException.class)
                    .hasMessageContaining("Only a shipped shipment");

            assertThat(shipment.getShipmentEmailSentAt()).isNull();
        }

        @Test
        void shouldRejectRecord_whenSentDateIsMissing() {
            Shipment shipment = shippedShipment();

            assertThatThrownBy(() -> shipment.markShipmentEmailAsSent(null))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessage("sentAt must not be null");

            assertThat(shipment.getShipmentEmailSentAt()).isNull();
        }
    }

    // ---------------------------------------------- registerEmailDeliveryFailure

    @Nested
    class RegisterEmailDeliveryFailure {

        @Test
        void shouldCountAttemptAndScheduleRetry() {
            Shipment shipment = shippedShipment();

            shipment.registerEmailDeliveryFailure(NEXT_ATTEMPT_AT);

            assertThat(shipment.getShipmentEmailAttempts()).isEqualTo(1);
            assertThat(shipment.getShipmentEmailNextAttemptAt()).isEqualTo(NEXT_ATTEMPT_AT);
        }

        @Test
        void shouldAccumulateAttempts_whenDeliveryFailsAgain() {
            Shipment shipment = shippedShipment();

            shipment.registerEmailDeliveryFailure(NEXT_ATTEMPT_AT);
            shipment.registerEmailDeliveryFailure(LATER_ATTEMPT_AT);

            assertThat(shipment.getShipmentEmailAttempts()).isEqualTo(2);
            assertThat(shipment.getShipmentEmailNextAttemptAt()).isEqualTo(LATER_ATTEMPT_AT);
        }

        @Test
        void shouldNotRecordDelivery() {
            Shipment shipment = shippedShipment();

            shipment.registerEmailDeliveryFailure(NEXT_ATTEMPT_AT);

            assertThat(shipment.getShipmentEmailSentAt()).isNull();
        }

        @Test
        void shouldRejectRegistration_whenNextAttemptDateIsMissing() {
            Shipment shipment = shippedShipment();

            assertThatThrownBy(() -> shipment.registerEmailDeliveryFailure(null))
                    .isInstanceOf(InvalidShipmentException.class)
                    .hasMessage("nextAttemptAt must not be null");

            assertThat(shipment.getShipmentEmailAttempts()).isZero();
        }
    }

    // ---------------------------------------------------------------- identidade

    @Nested
    class Identity {

        @Test
        void shouldEqualItself_whenNotPersisted() {
            Shipment shipment = preparingShipment();

            assertThat(shipment).isEqualTo(shipment);
        }

        /** Sem id atribuído não há identidade a comparar, e dois agregados distintos não podem colidir. */
        @Test
        void shouldNotEqualAnotherShipment_whenNeitherIsPersisted() {
            assertThat(preparingShipment()).isNotEqualTo(preparingShipment());
        }

        @Test
        void shouldKeepConstantHashCode_whenStatusChanges() {
            Shipment shipment = preparingShipment();
            int beforeDispatch = shipment.hashCode();

            shipment.markAsShipped(TRACKING_CODE, CARRIER, SHIPPED_AT);

            assertThat(shipment.hashCode()).isEqualTo(beforeDispatch);
        }
    }

    // -------------------------------------------------------------------- helpers

    private static Shipment preparingShipment() {
        return Shipment.createPreparingShipment(
                ORDER_ID, BILLED_AT, CUSTOMER_ID, CUSTOMER_EMAIL, CUSTOMER_NAME, CORRELATION_ID);
    }

    private static Shipment shippedShipment() {
        Shipment shipment = preparingShipment();
        shipment.markAsShipped(TRACKING_CODE, CARRIER, SHIPPED_AT);

        return shipment;
    }

    private static Shipment canceledShipment() {
        Shipment shipment = preparingShipment();
        shipment.markAsCanceled(CANCELED_AT);

        return shipment;
    }
}
