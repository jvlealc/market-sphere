package io.github.jvlealc.marketsphere.orders.domain.model;

import io.github.jvlealc.marketsphere.orders.domain.exception.IllegalOrderStatusChangeException;
import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidOrderException;
import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidOrderStateException;
import io.github.jvlealc.marketsphere.orders.domain.exception.OrderRehydrationException;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.CancellationInitiator;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.PaymentType;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.CancellationInfo;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.CustomerSnapshot;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.PaymentInfo;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.BILLED;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.CANCELED;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.PAID;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.PAYMENT_ERROR;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.PAYMENT_PENDING;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.PREPARING_SHIPMENT;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OrderTest {

    private static final long CUSTOMER_ID = 1L;
    private static final long ORDER_ID = 100L;

    private static final Instant ORDER_DATE  = Instant.parse("2026-08-21T09:00:00Z");
    private static final Instant PAID_AT     = Instant.parse("2026-08-21T10:00:00Z");
    private static final Instant BILLED_AT   = Instant.parse("2026-08-21T11:00:00Z");
    private static final Instant SHIPPED_AT  = Instant.parse("2026-08-21T12:00:00Z");
    private static final Instant CANCELED_AT = Instant.parse("2026-08-21T13:00:00Z");

    private static final String PAYMENT_KEY   = "pk-1";
    private static final String RETRY_KEY     = "pk-2";
    private static final String INVOICE_ID    = "INV-1";
    private static final String TRACKING_CODE = "BR-2ijs7Su29DaA5";

    private static final BigDecimal EXPECTED_TOTAL = new BigDecimal("650.00");

    @Nested
    class CreateNew {

        @Test
        void shouldStartAwaitingPayment() {
            Order order = Order.createNew(CUSTOMER_ID, customerSnapshot(), paymentInfo(), items());

            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
            assertThat(order.getObservations()).contains("Awaiting payment");
            assertThat(order.getOrderDate()).isNotNull();
            assertThat(order.getId()).isNull();
        }

        @Test
        void shouldSumTheItemSubtotals() {
            Order order = Order.createNew(CUSTOMER_ID, customerSnapshot(), paymentInfo(), items());
            
            assertThat(order.getTotal()).isEqualByComparingTo(EXPECTED_TOTAL);
        }
        
        @Test
        void shouldFreezeCustomerAndPaymentData() {
            CustomerSnapshot customer = customerSnapshot();
            PaymentInfo payment = paymentInfo();
            List<OrderItem> items = items();
            
            Order order = Order.createNew(CUSTOMER_ID, customer, payment, items);
            
            assertThat(order.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(order.getCustomerSnapshot()).isEqualTo(customer);
            assertThat(order.getPaymentInfo()).isEqualTo(payment);
            assertThat(order.getOrderItems()).containsExactlyElementsOf(items);
        }

        @Test
        void shouldCarryNoFulfillmentData() {
            Order order = Order.createNew(CUSTOMER_ID, customerSnapshot(), paymentInfo(), items());

            assertThat(order.getPaidAt()).isNull();
            assertThat(order.getPaymentKey()).isNull();
            assertThat(order.getBilledAt()).isNull();
            assertThat(order.getInvoiceId()).isNull();
            assertThat(order.getShippedAt()).isNull();
            assertThat(order.getTrackingCode()).isNull();
            assertThat(order.getCancellationInfo()).isNull();
        }
        
        @Test
        void shouldExposeAnUnmodifiableItems() {
            Order order = Order.createNew(CUSTOMER_ID, customerSnapshot(), paymentInfo(), items());

            assertThat(order.getOrderItems()).isUnmodifiable();
        }

        @Test
        void shouldKeepItsOwnItems_whenSourceListIsMutatedAfterwards() {
            List<OrderItem> mutableItems = new ArrayList<>(items());
            Order order = Order.createNew(CUSTOMER_ID, customerSnapshot(), paymentInfo(), mutableItems);
            mutableItems.clear();

            assertThat(order.getOrderItems()).hasSize(2);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidCreationData")
        void shouldRejectTheOrder_whenCreationDataIsInvalid(
                String label,
                Long customerId,
                CustomerSnapshot snapshot,
                PaymentInfo payment,
                List<OrderItem> items,
                String expectedMessage
        ) {
            assertThatThrownBy(() -> Order.createNew(customerId, snapshot, payment, items))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining(expectedMessage);
        }

        static Stream<Arguments> invalidCreationData() {
            return Stream.of(
                    arguments("null customer id", null, customerSnapshot(), paymentInfo(), items(), "An order must contain customer ID"),
                    arguments("customer id zero", 0L, customerSnapshot(), paymentInfo(), items(), "valid customer ID"),
                    arguments("negative customer id", -1L, customerSnapshot(), paymentInfo(), items(), "valid customer ID"),
                    arguments("null customer snapshot", CUSTOMER_ID, null, paymentInfo(), items(), "freeze the customer data"),
                    arguments("null payment info", CUSTOMER_ID, customerSnapshot(), null, items(), "must contain payment information"),
                    arguments("null item list", CUSTOMER_ID, customerSnapshot(), paymentInfo(), null, "at least one item"),
                    arguments("empty item list", CUSTOMER_ID, customerSnapshot(), paymentInfo(), List.of(), "at least one item")
            );
        }
    }

    // ------------------------------------------------------ registerPaymentRequest

    @Nested
    class RegisterPaymentRequest {

        @Test
        void shouldRegisterTheKey_whenOrderIsAwaitingPayment() {
            Order order = pendingOrder();

            assertThat(order.registerPaymentRequest(PAYMENT_KEY)).isTrue();
            assertThat(order.getPaymentKey()).isEqualTo(PAYMENT_KEY);
            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
            assertThat(order.getObservations()).contains("Payment initiated");
        }

        @Test
        void shouldReportNoChange_whenSameKeyArrivesAgain() {
            Order order = orderAwaitingPayment();

            assertThat(order.registerPaymentRequest(PAYMENT_KEY)).isFalse();
        }

        /** A chave é gravada normalizada; a comparação precisa normalizar do mesmo jeito. */
        @Test
        void shouldReportNoChange_whenTheSameKeyArrivesWithSurroundingSpaces() {
            Order order = orderAwaitingPayment();

            assertThat(order.registerPaymentRequest("  " + PAYMENT_KEY + "  ")).isFalse();
        }

        @Test
        void shouldReportNoChange_whenTheSameKeyArrivesForAnAlreadyPaidOrder() {
            Order order = paidOrder();

            assertThat(order.registerPaymentRequest(PAYMENT_KEY)).isFalse();
        }

        @Test
        void shouldRejectRequest_whenADifferentKeyArrivesForAnAlreadyPaidOrder() {
            Order order = paidOrder();

            assertThatThrownBy(() -> order.registerPaymentRequest(RETRY_KEY))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("Only PAYMENT_PENDING or PAYMENT_ERROR orders can initiate payment");
        }

        @Test
        void shouldReturnOrderToPending_whenANewKeyIsRegisteredAfterFailure() {
            Order order = paymentFailedOrder();

            assertThat(order.registerPaymentRequest(RETRY_KEY)).isTrue();
            assertThat(order.getPaymentKey()).isEqualTo(RETRY_KEY);
            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
        }

        @Test
        void shouldReportNoChange_whenFailedKeyArrivesAgain() {
            Order order = paymentFailedOrder();

            assertThat(order.registerPaymentRequest(PAYMENT_KEY)).isFalse();
            assertThat(order.getStatus()).isEqualTo(PAYMENT_ERROR);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "    ")
        void shouldRejectRequest_whenKeyIsBlank(String blankKey) {
            Order order = pendingOrder();

            assertThatThrownBy(() -> order.registerPaymentRequest(blankKey))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Payment key");
        }

        @Test
        void shouldReplaceKey_whenDifferentKeyArrivesForPendingOrder() {
            Order order = orderAwaitingPayment();

            assertThat(order.registerPaymentRequest(RETRY_KEY)).isTrue();
            assertThat(order.getPaymentKey()).isEqualTo(RETRY_KEY);
            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
        }

        /** Guarda de cancelamento é executado antes da validação da chave */
        @Test
        void shouldRejectRequest_whenOrderIsCancelled() {
            Order order = canceledOrder();

            assertThatThrownBy(() -> order.registerPaymentRequest(null))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("The order has been cancelled");
        }
    }

    // ----------------------------------------------------------------- markAsPaid

    @Nested
    class MarkAsPaid {

        @Test
        void shouldConfirmPayment() {
            Order order = orderAwaitingPayment();

            assertThat(order.markAsPaid(PAYMENT_KEY, PAID_AT)).isTrue();
            assertThat(order.getStatus()).isEqualTo(PAID);
            assertThat(order.getPaidAt()).isEqualTo(PAID_AT);
            assertThat(order.getObservations()).isEqualTo("Payment successfully confirmed");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PAID", "BILLED", "PREPARING_SHIPMENT", "SHIPPED"})
        void shouldReportNoChange_whenPaymentWasAlreadyConfirmed(OrderStatus status) {
            Order order = orderInStatus(status);

            assertThat(order.markAsPaid(PAYMENT_KEY, PAID_AT)).isFalse();
            assertThat(order.getStatus()).isEqualTo(status);
        }

        /** Fecha o ciclo da retentativa: registrar nova chave após falha, permite confirmar o pagamento. */
        @Test
        void shouldConfirmPayment_whenRetryWasRegisteredAfterFailure() {
            Order order = paymentFailedOrder();
            order.registerPaymentRequest(RETRY_KEY);

            assertThat(order.markAsPaid(RETRY_KEY, PAID_AT)).isTrue();
            assertThat(order.getStatus()).isEqualTo(PAID);
            assertThat(order.getPaymentKey()).isEqualTo(RETRY_KEY);
        }

        @Test
        void shouldRejectConfirmation_whenNoRetryWasRegisteredAfterFailure() {
            Order order = paymentFailedOrder();

            assertThatThrownBy(() -> order.markAsPaid(PAYMENT_KEY, PAID_AT))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("Only PAYMENT_PENDING orders can be marked as PAID");
        }

        @Test
        void shouldRejectConfirmation_whenNoPaymentKeyWasRegistered() {
            Order order = pendingOrder();

            assertThatThrownBy(() -> order.markAsPaid(PAYMENT_KEY, PAID_AT))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("does not match the registered payment request");

            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
            assertThat(order.getPaidAt()).isNull();
        }

        /** A confirmação de uma tentativa superada não pode pagar o pedido da tentativa vigente. */
        @Test
        void shouldRejectConfirmation_whenKeyDoesNotMatchTheRegisteredRequest() {
            Order order = orderAwaitingPayment();

            assertThatThrownBy(() -> order.markAsPaid(RETRY_KEY, PAID_AT))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("does not match the registered payment request");

            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
            assertThat(order.getPaidAt()).isNull();
        }

        /** Duas transações confirmando o mesmo pedido: possível cobrança dupla, nunca um no-op. */
        @Test
        void shouldRejectConfirmation_whenConflictingKeyArrivesForAnAlreadyPaidOrder() {
            Order order = paidOrder();

            assertThatThrownBy(() -> order.markAsPaid(RETRY_KEY, PAID_AT))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Conflicting payment data");

            assertThat(order.getPaymentKey()).isEqualTo(PAYMENT_KEY);
        }

        @Test
        void shouldReportNoChange_whenSameKeyArrivesWithSurroundingSpaces() {
            Order order = paidOrder();

            assertThat(order.markAsPaid("  " + PAYMENT_KEY + "  ", PAID_AT)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectConfirmation_whenKeyIsBlank(String blankKey) {
            Order order = orderAwaitingPayment();

            assertThatThrownBy(() -> order.markAsPaid(blankKey, PAID_AT))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Payment key");
        }

        @Test
        void shouldRejectConfirmation_whenOrderIsCanceled() {
            Order order = canceledOrder();

            assertThatThrownBy(() -> order.markAsPaid(PAYMENT_KEY, PAID_AT))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("The order has been cancelled");
        }

        @Test
        void shouldKeepPendingState_whenPaymentDateIsMissing() {
            Order order = orderAwaitingPayment();

            assertThatThrownBy(() -> order.markAsPaid(PAYMENT_KEY, null))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Paid at");

            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
            assertThat(order.getPaidAt()).isNull();
        }

        /** No ramo idempotente a data nem chega a ser validada. */
        @Test
        void shouldReportNoChange_whenDateIsMissingButPaymentWasAlreadyConfirmed() {
            Order order = paidOrder();

            assertThat(order.markAsPaid(PAYMENT_KEY, null)).isFalse();
            assertThat(order.getPaidAt()).isEqualTo(PAID_AT);
        }
    }

    // -------------------------------------------------------- markPaymentAsFailed

    @Nested
    class MarkPaymentAsFailed {

        @Test
        void shouldRecordFailure() {
            Order order = orderAwaitingPayment();

            assertThat(order.markPaymentAsFailed(PAYMENT_KEY, "Card declined")).isTrue();
            assertThat(order.getStatus()).isEqualTo(PAYMENT_ERROR);
            assertThat(order.getObservations()).isEqualTo("Card declined");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldFallBackToDefaultReason_whenNoneIsGiven(String blankReason) {
            Order order = orderAwaitingPayment();

            assertThat(order.markPaymentAsFailed(PAYMENT_KEY, blankReason)).isTrue();
            assertThat(order.getObservations()).isEqualTo("Payment failed");
        }

        /** O motivo original não é sobrescrito pelo redelivery. */
        @Test
        void shouldReportNoChange_whenFailureArrivesAgain() {
            Order order = paymentFailedOrder();

            assertThat(order.markPaymentAsFailed(PAYMENT_KEY, "Insufficient funds")).isFalse();
            assertThat(order.getObservations()).isEqualTo("Card declined");
        }

        @Test
        void shouldRecordTheFailure_whenRetryFailsAgain() {
            Order order = paymentFailedOrder();
            order.registerPaymentRequest(RETRY_KEY);

            assertThat(order.markPaymentAsFailed(RETRY_KEY, "Insufficient funds")).isTrue();
            assertThat(order.getStatus()).isEqualTo(PAYMENT_ERROR);
            assertThat(order.getObservations()).isEqualTo("Insufficient funds");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PAID", "BILLED", "PREPARING_SHIPMENT", "SHIPPED"})
        void shouldReportNoChange_whenPaymentWasAlreadyConfirmed(OrderStatus status) {
            Order order = orderInStatus(status);

            assertThat(order.markPaymentAsFailed(PAYMENT_KEY, "Card declined")).isFalse();
            assertThat(order.getStatus()).isEqualTo(status);
        }

        @Test
        void shouldRejectFailure_whenKeyDoesNotMatchTheRegisteredRequest() {
            Order order = orderAwaitingPayment();

            assertThatThrownBy(() -> order.markPaymentAsFailed(RETRY_KEY, "Card declined"))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("does not match the registered payment request");

            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
            assertThat(order.getObservations()).contains("Payment initiated");
        }

        @Test
        void shouldRejectFailure_whenNoPaymentKeyWasRegistered() {
            Order order = pendingOrder();

            assertThatThrownBy(() -> order.markPaymentAsFailed(PAYMENT_KEY, "Card declined"))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("does not match the registered payment request");

            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectFailure_whenKeyIsBlank(String blankKey) {
            Order order = orderAwaitingPayment();

            assertThatThrownBy(() -> order.markPaymentAsFailed(blankKey, "Card declined"))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Payment key");
        }

        @Test
        void shouldTruncateReason_whenExceedsStoredLength() {
            Order order = orderAwaitingPayment();

            order.markPaymentAsFailed(PAYMENT_KEY, "x".repeat(600));

            assertThat(order.getObservations()).hasSize(500);
        }

        @Test
        void shouldTrimReason_whenHasSurroundingSpaces() {
            Order order = orderAwaitingPayment();

            order.markPaymentAsFailed(PAYMENT_KEY, "   Card declined   ");

            assertThat(order.getObservations()).isEqualTo("Card declined");
        }

        @Test
        void shouldRejectFailure_whenOrderIsCanceled() {
            Order order = canceledOrder();

            assertThatThrownBy(() -> order.markPaymentAsFailed(PAYMENT_KEY, "Card declined"))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("The order has been cancelled");
        }
    }

    // --------------------------------------------------------------- markAsBilled

    @Nested
    class MarkAsBilled {

        @Test
        void shouldBillOrder() {
            Order order = paidOrder();

            assertThat(order.markAsBilled(INVOICE_ID, BILLED_AT)).isTrue();
            assertThat(order.getStatus()).isEqualTo(BILLED);
            assertThat(order.getInvoiceId()).isEqualTo(INVOICE_ID);
            assertThat(order.getBilledAt()).isEqualTo(BILLED_AT);
            assertThat(order.getObservations()).isEqualTo("Order successfully billed");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"BILLED", "PREPARING_SHIPMENT", "SHIPPED"})
        void shouldReportNoChange_whenSameInvoiceArrivesAgain(OrderStatus status) {
            Order order = orderInStatus(status);

            assertThat(order.markAsBilled(INVOICE_ID, BILLED_AT)).isFalse();
            assertThat(order.getStatus()).isEqualTo(status);
        }

        @Test
        void shouldReportNoChange_whenSameInvoiceArrivesWithSurroundingSpaces() {
            Order order = billedOrder();

            assertThat(order.markAsBilled("  " + INVOICE_ID + "  ", BILLED_AT)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"BILLED", "PREPARING_SHIPMENT", "SHIPPED"})
        void shouldRejectBilling_whenConflictingInvoiceArrives(OrderStatus status) {
            Order order = orderInStatus(status);

            assertThatThrownBy(() -> order.markAsBilled("INV-2", BILLED_AT))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Conflicting billing data");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PAYMENT_PENDING", "PAYMENT_ERROR"})
        void shouldRejectBilling_whenPaymentIsNotConfirmed(OrderStatus status) {
            Order order = orderInStatus(status);

            assertThatThrownBy(() -> order.markAsBilled(INVOICE_ID, BILLED_AT))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("Only PAID orders can be marked as BILLED");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectBilling_whenInvoiceIdIsBlank(String blankInvoiceId) {
            Order order = paidOrder();

            assertThatThrownBy(() -> order.markAsBilled(blankInvoiceId, BILLED_AT))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Invoice ID");
        }

        /** A validação do identificador vem antes da guarda de idempotência. */
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectBilling_whenInvoiceIdIsBlankForAnAlreadyBilledOrder(String blankInvoiceId) {
            Order order = billedOrder();

            assertThatThrownBy(() -> order.markAsBilled(blankInvoiceId, BILLED_AT))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Invoice ID");
        }

        @Test
        void shouldKeepPaidState_whenBillingDateIsMissing() {
            Order order = paidOrder();

            assertThatThrownBy(() -> order.markAsBilled(INVOICE_ID, null))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Billed at");

            assertThat(order.getStatus()).isEqualTo(PAID);
            assertThat(order.getInvoiceId()).isNull();
            assertThat(order.getBilledAt()).isNull();
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PREPARING_SHIPMENT", "SHIPPED"})
        void shouldRejectBilling_whenInvoiceIdIsBlankForOrderBeyondBilled(OrderStatus status) {
            Order order = orderInStatus(status);

            assertThatThrownBy(() -> order.markAsBilled("   ", BILLED_AT))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Invoice ID");

            assertThat(order.getStatus()).isEqualTo(status);
        }

        @Test
        void shouldRejectBilling_whenOrderIsCanceled() {
            Order order = canceledOrder();

            assertThatThrownBy(() -> order.markAsBilled(null, null))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("The order has been cancelled");
        }
    }

    // ----------------------------------------------------- markAsPreparingShipment

    @Nested
    class MarkAsPreparingShipment {

        @Test
        void shouldStartPreparingShipment() {
            Order order = billedOrder();

            assertThat(order.markAsPreparingShipment()).isTrue();
            assertThat(order.getStatus()).isEqualTo(PREPARING_SHIPMENT);
            assertThat(order.getObservations()).isEqualTo("The order is being prepared for shipment");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PREPARING_SHIPMENT", "SHIPPED"})
        void shouldReportNoChange_whenShipmentIsAlreadyUnderway(OrderStatus status) {
            Order order = orderInStatus(status);

            assertThat(order.markAsPreparingShipment()).isFalse();
            assertThat(order.getStatus()).isEqualTo(status);
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PAYMENT_PENDING", "PAYMENT_ERROR", "PAID"})
        void shouldRejectPreparingShipment_whenOrderIsNotBilled(OrderStatus status) {
            Order order = orderInStatus(status);

            assertThatThrownBy(order::markAsPreparingShipment)
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("Only BILLED orders can be marked as PREPARING_SHIPMENT");
        }

        @Test
        void shouldRejectPreparingShipment_whenOrderIsCanceled() {
            Order order = canceledOrder();

            assertThatThrownBy(order::markAsPreparingShipment)
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("The order has been cancelled");
        }
    }

    // -------------------------------------------------------------- markAsShipped

    @Nested
    class MarkAsShipped {

        @Test
        void shouldShipOrder() {
            Order order = preparingShipmentOrder();

            assertThat(order.markAsShipped(TRACKING_CODE, SHIPPED_AT)).isTrue();
            assertThat(order.getStatus()).isEqualTo(SHIPPED);
            assertThat(order.getTrackingCode()).isEqualTo(TRACKING_CODE);
            assertThat(order.getShippedAt()).isEqualTo(SHIPPED_AT);
            assertThat(order.getObservations()).isEqualTo("Order successfully shipped");
        }

        @Test
        void shouldReportNoChange_whenSameTrackingCodeArrivesAgain() {
            Order order = shippedOrder();

            assertThat(order.markAsShipped(TRACKING_CODE, SHIPPED_AT)).isFalse();
        }

        @Test
        void shouldReportNoChange_whenSameTrackingCodeArrivesWithSurroundingSpaces() {
            Order order = shippedOrder();

            assertThat(order.markAsShipped("  " + TRACKING_CODE + "  ", SHIPPED_AT)).isFalse();
        }

        @Test
        void shouldRejectShipping_whenConflictingTrackingCodeArrives() {
            Order order = shippedOrder();

            assertThatThrownBy(() -> order.markAsShipped("BR-OTHER", SHIPPED_AT))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Conflicting shipping data");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PAYMENT_PENDING", "PAYMENT_ERROR", "PAID", "BILLED"})
        void shouldRejectShipping_whenShipmentWasNotPrepared(OrderStatus status) {
            Order order = orderInStatus(status);

            assertThatThrownBy(() -> order.markAsShipped(TRACKING_CODE, SHIPPED_AT))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("Only PREPARING_SHIPMENT orders can be marked as SHIPPED");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectTheShipping_whenTheTrackingCodeIsBlank(String blankTrackingCode) {
            Order order = preparingShipmentOrder();

            assertThatThrownBy(() -> order.markAsShipped(blankTrackingCode, SHIPPED_AT))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Tracking code");
        }

        @Test
        void shouldKeepPreparingShipmentState_whenShippingDateIsMissing() {
            Order order = preparingShipmentOrder();

            assertThatThrownBy(() -> order.markAsShipped(TRACKING_CODE, null))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Shipped at");

            assertThat(order.getStatus()).isEqualTo(PREPARING_SHIPMENT);
            assertThat(order.getTrackingCode()).isNull();
            assertThat(order.getShippedAt()).isNull();
        }

        @Test
        void shouldRejectShipping_whenOrderIsCanceled() {
            Order order = canceledOrder();

            assertThatThrownBy(() -> order.markAsShipped(null, null))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("The order has been cancelled");
        }
    }

    // --------------------------------------------------------------------- cancel

    @Nested
    class Cancel {

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"SHIPPED", "CANCELED"}, mode = EnumSource.Mode.EXCLUDE)
        void shouldCancelOrder_whenOrderIsNotShipped(OrderStatus status) {
            Order order = orderInStatus(status);
            CancellationInfo info = cancellationInfo();

            order.cancel(info);

            assertThat(order.getStatus()).isEqualTo(CANCELED);
            assertThat(order.getCancellationInfo()).isEqualTo(info);
            assertThat(order.getObservations()).isEqualTo("Order canceled");
        }

        /** Cancelar preserva os dados que o pedido já tinha */
        @Test
        void shouldPreserveEarlierStages_whenBilledOrderIsCanceled() {
            Order order = billedOrder();

            order.cancel(cancellationInfo());

            assertThat(order.getPaidAt()).isEqualTo(PAID_AT);
            assertThat(order.getInvoiceId()).isEqualTo(INVOICE_ID);
            assertThat(order.getShippedAt()).isNull();
            assertThat(order.getTrackingCode()).isNull();
        }

        @Test
        void shouldRejectCancellation_whenOrderHasAlreadyShipped() {
            Order order = shippedOrder();

            assertThatThrownBy(() -> order.cancel(cancellationInfo()))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("cannot be canceled if it has been SHIPPED");
        }

        /** Diferente das demais operações, cancelar duas vezes lança exceção. */
        @Test
        void shouldRejectCancellation_whenOrderIsAlreadyCanceled() {
            Order order = canceledOrder();

            assertThatThrownBy(() -> order.cancel(cancellationInfo()))
                    .isInstanceOf(IllegalOrderStatusChangeException.class)
                    .hasMessageContaining("The order has been cancelled");
        }

        @Test
        void shouldKeepCurrentState_whenCancellationInfoIsMissing() {
            Order order = paidOrder();

            assertThatThrownBy(() -> order.cancel(null))
                    .isInstanceOf(InvalidOrderException.class)
                    .hasMessageContaining("Cancellation info");

            assertThat(order.getStatus()).isEqualTo(PAID);
            assertThat(order.getCancellationInfo()).isNull();
        }
    }

    // ------------------------------------------------------------------ rehydrate

    @Nested
    class Rehydrate {

        @ParameterizedTest
        @EnumSource(OrderStatus.class)
        void shouldRestoreOrder_whenStoredStateIsCoherent(OrderStatus status) {
            Order order = coherentStoredOrder(status);

            assertThat(order.getId()).isEqualTo(ORDER_ID);
            assertThat(order.getStatus()).isEqualTo(status);
            assertThat(order.getTotal()).isEqualByComparingTo(EXPECTED_TOTAL);
            assertThat(order.getOrderItems()).isUnmodifiable();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("incompleteRehydratedOrders")
        void shouldRejectRehydration_whenRequiredFieldIsMissing(
                String label,
                ThrowingCallable rehydration,
                String expectedMessage
        ) {
            assertThatThrownBy(rehydration)
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining(expectedMessage);
        }

        static Stream<Arguments> incompleteRehydratedOrders() {
            return Stream.of(
                    arguments(
                            "missing id",
                            rehydrationOf(
                                    null, CUSTOMER_ID, customerSnapshot(), ORDER_DATE, PAID, EXPECTED_TOTAL,
                                    storedPaymentInfo(), storedItems()
                            ),
                            "must have an ID"
                    ),
                    arguments(
                            "missing customer id",
                            rehydrationOf(
                                    ORDER_ID, null, customerSnapshot(), ORDER_DATE, PAID, EXPECTED_TOTAL,
                                    storedPaymentInfo(), storedItems()
                            ),
                            "must belong to a customer"
                    ),
                    arguments(
                            "customer id zero",
                            rehydrationOf(
                                    ORDER_ID, 0L, customerSnapshot(), ORDER_DATE, PAID, EXPECTED_TOTAL,
                                    storedPaymentInfo(), storedItems()
                            ),
                            "valid customer ID"
                    ),
                    arguments(
                            "missing customer snapshot",
                            rehydrationOf(
                                    ORDER_ID, CUSTOMER_ID, null, ORDER_DATE, PAID, EXPECTED_TOTAL,
                                    storedPaymentInfo(), storedItems()
                            ),
                            "customer data frozen"
                    ),
                    arguments(
                            "missing order date",
                            rehydrationOf(
                                    ORDER_ID, CUSTOMER_ID, customerSnapshot(), null, PAID, EXPECTED_TOTAL,
                                    storedPaymentInfo(), storedItems()
                            ),
                            "must have an order date"
                    ),
                    arguments(
                            "missing status",
                            rehydrationOf(
                                    ORDER_ID, CUSTOMER_ID, customerSnapshot(), ORDER_DATE, null, EXPECTED_TOTAL,
                                    storedPaymentInfo(), storedItems()
                            ),
                            "must have a status"
                    ),
                    arguments(
                            "missing total",
                            rehydrationOf(
                                    ORDER_ID, CUSTOMER_ID, customerSnapshot(), ORDER_DATE, PAID, null,
                                    storedPaymentInfo(), storedItems()
                            ),
                            "must have a total value"
                    ),
                    arguments(
                            "missing payment info",
                            rehydrationOf(
                                    ORDER_ID, CUSTOMER_ID, customerSnapshot(), ORDER_DATE, PAID, EXPECTED_TOTAL,
                                    null, storedItems()
                            ),
                            "must contain payment information"
                    ),
                    arguments(
                            "missing items",
                            rehydrationOf(
                                    ORDER_ID, CUSTOMER_ID, customerSnapshot(), ORDER_DATE, PAID, EXPECTED_TOTAL,
                                    storedPaymentInfo(), null
                            ),
                            "at least one item"
                    ),
                    arguments(
                            "empty items",
                            rehydrationOf(
                                    ORDER_ID, CUSTOMER_ID, customerSnapshot(), ORDER_DATE, PAID, EXPECTED_TOTAL,
                                    storedPaymentInfo(), List.of()
                            ),
                            "at least one item"
                    )
            );
        }

        // --- Alcançou o marco, exige os campos

        @Test
        void shouldRejectRehydration_whenPaidOrderHasNoPaymentKey() {
            assertThatThrownBy(() -> rehydrateWith(PAID, PAID_AT, null, null, null, null, null, null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must have")
                    .hasMessageContaining("payment key");
        }

        @Test
        void shouldRejectRehydration_whenPaidOrderHasBlankPaymentKey() {
            assertThatThrownBy(() -> rehydrateWith(PAID, PAID_AT, null, null, "   ", null, null, null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must have")
                    .hasMessageContaining("payment key");
        }

        @Test
        void shouldRejectRehydration_whenPaidOrderHasNoPaidDate() {
            assertThatThrownBy(() -> rehydrateWith(PAID, null, null, null, PAYMENT_KEY, null, null, null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must have")
                    .hasMessageContaining("paid date");
        }

        @Test
        void shouldRejectRehydration_whenBilledOrderHasNoBilledDate() {
            assertThatThrownBy(() -> rehydrateWith(BILLED, PAID_AT, null, null, PAYMENT_KEY, null, INVOICE_ID, null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must have")
                    .hasMessageContaining("billing data");
        }

        @Test
        void shouldRejectRehydration_whenBilledOrderHasBlankInvoiceId() {
            assertThatThrownBy(() -> rehydrateWith(BILLED, PAID_AT, BILLED_AT, null, PAYMENT_KEY, null, "   ", null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must have")
                    .hasMessageContaining("billing data");
        }

        @Test
        void shouldRejectRehydration_whenShippedOrderHasNoShippedDate() {
            assertThatThrownBy(() -> rehydrateWith(SHIPPED, PAID_AT, BILLED_AT, null, PAYMENT_KEY, TRACKING_CODE, INVOICE_ID, null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must have")
                    .hasMessageContaining("shipping data");
        }

        @Test
        void shouldRejectRehydration_whenShippedOrderHasBlankTrackingCode() {
            assertThatThrownBy(() -> rehydrateWith(SHIPPED, PAID_AT, BILLED_AT, SHIPPED_AT, PAYMENT_KEY, "   ", INVOICE_ID, null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must have")
                    .hasMessageContaining("shipping data");
        }

        @Test
        void shouldRejectRehydration_whenCanceledOrderHasNoCancellationInfo() {
            assertThatThrownBy(() -> rehydrateWith(CANCELED, null, null, null, null, null, null, null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("cancellation information");
        }

        // --- Não alcançou o marco, proíbe os campos

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PAYMENT_PENDING", "PAYMENT_ERROR"})
        void shouldRejectRehydration_whenUnpaidOrderCarriesPaidDate(OrderStatus status) {
            assertThatThrownBy(() -> rehydrateWith(status, PAID_AT, null, null, PAYMENT_KEY, null, null, null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must not have")
                    .hasMessageContaining("paid date");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PAYMENT_PENDING", "PAYMENT_ERROR", "PAID"})
        void shouldRejectRehydration_whenUnbilledOrderCarriesBillingData(OrderStatus status) {
            assertThatThrownBy(() -> rehydrateWith(
                    status, paidDateFor(status), BILLED_AT, null, PAYMENT_KEY, null, INVOICE_ID, null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must not have")
                    .hasMessageContaining("billing data");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class,
                names = {"PAYMENT_PENDING", "PAYMENT_ERROR", "PAID", "BILLED", "PREPARING_SHIPMENT"})
        void shouldRejectRehydration_whenUnshippedOrderCarriesShippingData(OrderStatus status) {
            assertThatThrownBy(() -> rehydrateWith(
                    status, paidDateFor(status), billedDateFor(status), SHIPPED_AT,
                    PAYMENT_KEY, TRACKING_CODE, invoiceIdFor(status), null))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must not have")
                    .hasMessageContaining("shipping data");
        }

        /** A chave de pagamento é a exceção deliberada: PAYMENT_PENDING pode tê-la. */
        @Test
        void shouldRestorePendingOrder_whenItAlreadyHasRegisteredPaymentKey() {
            Order order = rehydrateWith(PAYMENT_PENDING, null, null, null, PAYMENT_KEY, null, null, null);

            assertThat(order.getPaymentKey()).isEqualTo(PAYMENT_KEY);
            assertThat(order.getStatus()).isEqualTo(PAYMENT_PENDING);
        }

        // --- CANCELED: preserva o histórico

        @Test
        void shouldRestoreCanceledOrder_whenItPreservesEarlierStagesHistory() {
            Order order = rehydrateWith(CANCELED, PAID_AT, BILLED_AT, null, PAYMENT_KEY, null, INVOICE_ID, storedCancellationInfo());

            assertThat(order.getStatus()).isEqualTo(CANCELED);
            assertThat(order.getInvoiceId()).isEqualTo(INVOICE_ID);
        }

        @Test
        void shouldRestoreCanceledOrder_whenItCarriesNoHistory() {
            Order order = rehydrateWith(CANCELED, null, null, null, null, null, null, storedCancellationInfo());

            assertThat(order.getStatus()).isEqualTo(CANCELED);
            assertThat(order.getPaidAt()).isNull();
        }

        /** Cancelar a partir de SHIPPED é impossível, então dado de envio em CANCELED é estado impossível. */
        @Test
        void shouldRejectRehydration_whenCanceledOrderCarriesShippingData() {
            assertThatThrownBy(() -> rehydrateWith(
                    CANCELED, PAID_AT, BILLED_AT, SHIPPED_AT, PAYMENT_KEY, TRACKING_CODE, INVOICE_ID, storedCancellationInfo()))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("must not have")
                    .hasMessageContaining("shipping data");
        }

        @Test
        void shouldRejectRehydration_whenCanceledOrderWasBilledWithoutBeingPaid() {
            assertThatThrownBy(() -> rehydrateWith(
                    CANCELED, null, BILLED_AT, null, PAYMENT_KEY, null, INVOICE_ID, storedCancellationInfo()))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("paid date");
        }

        @Test
        void shouldRejectRehydration_whenCanceledOrderHasABilledDateWithoutInvoiceId() {
            assertThatThrownBy(() -> rehydrateWith(
                    CANCELED, PAID_AT, BILLED_AT, null, PAYMENT_KEY, null, null, storedCancellationInfo()))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("billing data");
        }

        @Test
        void shouldRejectRehydration_whenCanceledOrderHasPaidDateWithoutPaymentKey() {
            assertThatThrownBy(() -> rehydrateWith(
                    CANCELED, PAID_AT, null, null, null, null, null, storedCancellationInfo()))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("payment key");
        }

        @Test
        void shouldRejectRehydration_whenTotalIsNegative() {
            assertThatThrownBy(rehydrationOf(
                    ORDER_ID, CUSTOMER_ID, customerSnapshot(), ORDER_DATE, PAID, new BigDecimal("-0.01"),
                    storedPaymentInfo(), storedItems()))
                    .isInstanceOf(OrderRehydrationException.class)
                    .hasMessageContaining("negative total value");
        }

        @Test
        void shouldRestoreOrder_whenTotalIsZero() {
            Order order = Order.rehydrate(
                    ORDER_ID, CUSTOMER_ID, customerSnapshot(), ORDER_DATE,
                    null, null, null, PAYMENT_KEY, "stored observations", PAYMENT_PENDING,
                    BigDecimal.ZERO, null, null, storedPaymentInfo(), storedItems(), null);

            assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldRestoreObservations() {
            Order order = rehydrateWith(PAYMENT_PENDING, null, null, null, PAYMENT_KEY, null, null, null);

            assertThat(order.getObservations()).isEqualTo("stored observations");
        }
    }

    // ------------------------------------------------------ helpers

    private static Order pendingOrder() {
        return Order.createNew(CUSTOMER_ID, customerSnapshot(), paymentInfo(), items());
    }

    private static Order orderAwaitingPayment() {
        Order order = pendingOrder();
        order.registerPaymentRequest(PAYMENT_KEY);
        return order;
    }

    private static Order paymentFailedOrder() {
        Order order = orderAwaitingPayment();
        order.markPaymentAsFailed(PAYMENT_KEY, "Card declined");
        return order;
    }

    private static Order paidOrder() {
        Order order = orderAwaitingPayment();
        order.markAsPaid(PAYMENT_KEY, PAID_AT);
        return order;
    }

    private static Order billedOrder() {
        Order order = paidOrder();
        order.markAsBilled(INVOICE_ID, BILLED_AT);
        return order;
    }

    private static Order preparingShipmentOrder() {
        Order order = billedOrder();
        order.markAsPreparingShipment();
        return order;
    }

    private static Order shippedOrder() {
        Order order = preparingShipmentOrder();
        order.markAsShipped(TRACKING_CODE, SHIPPED_AT);
        return order;
    }

    private static Order canceledOrder() {
        Order order = pendingOrder();
        order.cancel(cancellationInfo());
        return order;
    }

    private static Order orderInStatus(OrderStatus status) {
        return switch (status) {
            case PAYMENT_PENDING -> orderAwaitingPayment();
            case PAYMENT_ERROR -> paymentFailedOrder();
            case PAID -> paidOrder();
            case BILLED -> billedOrder();
            case PREPARING_SHIPMENT -> preparingShipmentOrder();
            case SHIPPED -> shippedOrder();
            case CANCELED -> canceledOrder();
        };
    }

    private static Order rehydrateWith(OrderStatus status, Instant paidAt, Instant billedAt, Instant shippedAt,
                                       String paymentKey, String trackingCode, String invoiceId,
                                       CancellationInfo cancellationInfo) {
        return Order.rehydrate(
                ORDER_ID, CUSTOMER_ID, customerSnapshot(), ORDER_DATE,
                paidAt, billedAt, shippedAt, paymentKey, "stored observations", status,
                EXPECTED_TOTAL, trackingCode, invoiceId, storedPaymentInfo(), storedItems(), cancellationInfo
        );
    }

    private static ThrowingCallable rehydrationOf(Long id, Long customerId, CustomerSnapshot snapshot,
                                                  Instant orderDate, OrderStatus status, BigDecimal total,
                                                  PaymentInfo paymentInfo, List<OrderItem> orderItems) {
        return () -> Order.rehydrate(
                id, customerId, snapshot, orderDate,
                PAID_AT, null, null, PAYMENT_KEY, "stored observations", status,
                total, null, null, paymentInfo, orderItems, null
        );
    }

    private static Order coherentStoredOrder(OrderStatus status) {
        return switch (status) {
            case PAYMENT_PENDING, PAYMENT_ERROR ->
                    rehydrateWith(status, null, null, null, PAYMENT_KEY, null, null, null);
            case PAID ->
                    rehydrateWith(status, PAID_AT, null, null, PAYMENT_KEY, null, null, null);
            case BILLED, PREPARING_SHIPMENT ->
                    rehydrateWith(status, PAID_AT, BILLED_AT, null, PAYMENT_KEY, null, INVOICE_ID, null);
            case SHIPPED ->
                    rehydrateWith(status, PAID_AT, BILLED_AT, SHIPPED_AT, PAYMENT_KEY, TRACKING_CODE, INVOICE_ID, null);
            case CANCELED ->
                    rehydrateWith(status, PAID_AT, BILLED_AT, null, PAYMENT_KEY, null, INVOICE_ID, storedCancellationInfo());
        };
    }

    private static Instant paidDateFor(OrderStatus status) {
        return status == PAYMENT_PENDING || status == PAYMENT_ERROR ? null : PAID_AT;
    }

    private static Instant billedDateFor(OrderStatus status) {
        return status == BILLED || status == PREPARING_SHIPMENT || status == SHIPPED ? BILLED_AT : null;
    }

    private static String invoiceIdFor(OrderStatus status) {
        return billedDateFor(status) == null ? null : INVOICE_ID;
    }

    private static CustomerSnapshot customerSnapshot() {
        return new CustomerSnapshot(
                "John Doe", "11122233344", "john.doe@example.com", "5571999998888",
                "41111222", "Rua Primavera", "1A", "", "Alves de Souza",
                "Paulo Afonso", "BA", "Brazil"
        );
    }

    private static PaymentInfo paymentInfo() {
        return PaymentInfo.createNew("4115", PaymentType.DEBIT);
    }

    private static PaymentInfo storedPaymentInfo() {
        return PaymentInfo.rehydrate("4115", PaymentType.DEBIT, ORDER_DATE);
    }

    private static List<OrderItem> items() {
        return List.of(
                OrderItem.createNew(1L, "Product 1", 1, new BigDecimal("150.00")),
                OrderItem.createNew(2L, "Product 2", 5, new BigDecimal("100.00"))
        );
    }

    private static List<OrderItem> storedItems() {
        return List.of(
                OrderItem.rehydrate(1L, 1L, "Product 1", 1, new BigDecimal("150.00")),
                OrderItem.rehydrate(2L, 2L, "Product 2", 5, new BigDecimal("100.00"))
        );
    }

    private static CancellationInfo cancellationInfo() {
        return CancellationInfo.createNew(CancellationInitiator.CUSTOMER, "Changed my mind");
    }

    private static CancellationInfo storedCancellationInfo() {
        return CancellationInfo.rehydrate(CancellationInitiator.ADMIN, "Fraud suspicion", CANCELED_AT);
    }
}
