package io.github.jvlealc.marketsphere.billing.domain.model;

import io.github.jvlealc.marketsphere.billing.domain.exception.IllegalInvoiceStatusChangeException;
import io.github.jvlealc.marketsphere.billing.domain.exception.InvalidInvoiceException;
import io.github.jvlealc.marketsphere.billing.domain.exception.InvalidInvoiceStateException;
import io.github.jvlealc.marketsphere.billing.domain.exception.InvoiceRehydrationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static io.github.jvlealc.marketsphere.billing.domain.model.InvoiceStatus.FAILED;
import static io.github.jvlealc.marketsphere.billing.domain.model.InvoiceStatus.GENERATED;
import static io.github.jvlealc.marketsphere.billing.domain.model.InvoiceStatus.PROCESSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class InvoiceTest {

    private static final UUID INVOICE_ID = UUID.fromString("019ff81e-2e41-7c42-b1fa-2a239481cb3e");
    private static final UUID OTHER_INVOICE_ID = UUID.fromString("01a05a27-32af-7c42-9d0e-5f11c8a7b204");
    private static final long ORDER_ID = 100L;

    private static final Instant GENERATED_AT       = Instant.parse("2026-09-01T10:00:00Z");
    private static final Instant OTHER_GENERATED_AT = Instant.parse("2026-09-01T10:30:00Z");
    private static final Instant FAILED_AT          = Instant.parse("2026-09-01T11:00:00Z");
    private static final Instant OTHER_FAILED_AT    = Instant.parse("2026-09-01T11:30:00Z");

    private static final String STORAGE_KEY       = "invoices/2026/09/019ff81e-2e41.pdf";
    private static final String OTHER_STORAGE_KEY = "invoices/2026/09/01a05a27-32af.pdf";

    private static final String FAILURE_REASON       = "JasperReports: template not found";
    private static final String OTHER_FAILURE_REASON = "MinIO: bucket unavailable";

    private static final int MAX_FAILURE_REASON_LENGTH = 2_000;

    // ------------------------------------------------------------------ createNew

    @Nested
    class CreateNew {

        @Test
        void shouldStartProcessing() {
            Invoice invoice = processingInvoice();

            assertThat(invoice.getStatus()).isEqualTo(PROCESSING);
            assertThat(invoice.getId()).isEqualTo(INVOICE_ID);
            assertThat(invoice.getOrderId()).isEqualTo(ORDER_ID);
        }

        @Test
        void shouldCarryNoGenerationOrFailureData() {
            Invoice invoice = processingInvoice();

            assertThat(invoice.getStorageKey()).isNull();
            assertThat(invoice.getGeneratedAt()).isNull();
            assertThat(invoice.getFailedAt()).isNull();
            assertThat(invoice.getFailureReason()).isNull();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidCreationData")
        void shouldRejectInvoice_whenCreationDataIsInvalid(
                String label,
                UUID id,
                Long orderId,
                String expectedMessage
        ) {
            assertThatThrownBy(() -> Invoice.createNew(id, orderId))
                    .isInstanceOf(InvalidInvoiceException.class)
                    .hasMessageContaining(expectedMessage);
        }

        static Stream<Arguments> invalidCreationData() {
            return Stream.of(
                    arguments("null invoice ID", null, ORDER_ID, "Invoice ID must not be null"),
                    arguments("null order ID", INVOICE_ID, null, "Order ID must not be null"),
                    arguments("order ID zero", INVOICE_ID, 0L, "Order ID must be greater than zero"),
                    arguments("negative order ID", INVOICE_ID, -1L, "Order ID must be greater than zero")
            );
        }
    }

    // ------------------------------------------------------------ markAsGenerated

    @Nested
    class MarkAsGenerated {

        @Test
        void shouldGenerateInvoice() {
            Invoice invoice = processingInvoice();

            assertThat(invoice.markAsGenerated(STORAGE_KEY, GENERATED_AT)).isTrue();
            assertThat(invoice.getStatus()).isEqualTo(GENERATED);
            assertThat(invoice.getStorageKey()).isEqualTo(STORAGE_KEY);
            assertThat(invoice.getGeneratedAt()).isEqualTo(GENERATED_AT);
        }

        @Test
        void shouldNormalizeStorageKey_whenItArrivesWithSurroundingSpaces() {
            Invoice invoice = processingInvoice();

            invoice.markAsGenerated("  " + STORAGE_KEY + "  ", GENERATED_AT);

            assertThat(invoice.getStorageKey()).isEqualTo(STORAGE_KEY);
        }

        @Test
        void shouldReportNoChange_whenSameStorageKeyArrivesAgain() {
            Invoice invoice = generatedInvoice();

            assertThat(invoice.markAsGenerated(STORAGE_KEY, GENERATED_AT)).isFalse();
            assertThat(invoice.getGeneratedAt()).isEqualTo(GENERATED_AT);
        }

        @Test
        void shouldReportNoChange_whenSameStorageKeyArrivesWithSurroundingSpaces() {
            Invoice invoice = generatedInvoice();

            assertThat(invoice.markAsGenerated("  " + STORAGE_KEY + "  ", GENERATED_AT)).isFalse();
        }

        /**
         * A chave é única no banco (`uq_invoices_storage_key`): duas chaves distintas para a mesma nota
         * significam dois PDFs gravados, e o segundo ficaria órfão se a confirmação passasse em silêncio.
         */
        @Test
        void shouldRejectGeneration_whenConflictingStorageKeyArrives() {
            Invoice invoice = generatedInvoice();

            assertThatThrownBy(() -> invoice.markAsGenerated(OTHER_STORAGE_KEY, GENERATED_AT))
                    .isInstanceOf(InvalidInvoiceStateException.class)
                    .hasMessageContaining("Conflicting generation data");

            assertThat(invoice.getStorageKey()).isEqualTo(STORAGE_KEY);
        }

        /** `generatedAt` não participa da comparação de conflito — só a chave identifica o documento. */
        @Test
        void shouldReportNoChange_whenOnlyGeneratedDateDiverges() {
            Invoice invoice = generatedInvoice();

            assertThat(invoice.markAsGenerated(STORAGE_KEY, OTHER_GENERATED_AT)).isFalse();
            assertThat(invoice.getGeneratedAt()).isEqualTo(GENERATED_AT);
        }

        /** `FAILED` é terminal: é a metade que impede a nota queimada de voltar a ser gerada. */
        @Test
        void shouldRejectGeneration_whenInvoiceHasFailed() {
            Invoice invoice = failedInvoice();

            assertThatThrownBy(() -> invoice.markAsGenerated(STORAGE_KEY, GENERATED_AT))
                    .isInstanceOf(InvalidInvoiceStateException.class)
                    .hasMessageContaining("Invoice has been failed");

            assertThat(invoice.getStatus()).isEqualTo(FAILED);
            assertThat(invoice.getStorageKey()).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectGeneration_whenStorageKeyIsBlank(String blankKey) {
            Invoice invoice = processingInvoice();

            assertThatThrownBy(() -> invoice.markAsGenerated(blankKey, GENERATED_AT))
                    .isInstanceOf(InvalidInvoiceException.class)
                    .hasMessageContaining("Storage key is required");

            assertThat(invoice.getStatus()).isEqualTo(PROCESSING);
        }

        @Test
        void shouldRejectGeneration_whenGeneratedDateIsMissing() {
            Invoice invoice = processingInvoice();

            assertThatThrownBy(() -> invoice.markAsGenerated(STORAGE_KEY, null))
                    .isInstanceOf(InvalidInvoiceException.class)
                    .hasMessageContaining("Generated at date is required");

            assertThat(invoice.getStatus()).isEqualTo(PROCESSING);
            assertThat(invoice.getStorageKey()).isNull();
        }

        /** A entrada é normalizada antes da guarda de idempotência, então chave em branco é recusada mesmo depois de gerada. */
        @Test
        void shouldRejectGeneration_whenStorageKeyIsBlankAfterGeneration() {
            Invoice invoice = generatedInvoice();

            assertThatThrownBy(() -> invoice.markAsGenerated("   ", GENERATED_AT))
                    .isInstanceOf(InvalidInvoiceException.class)
                    .hasMessageContaining("Storage key is required");
        }

        /** A guarda terminal vem antes da validação de argumento: uma nota queimada recusa qualquer entrada. */
        @Test
        void shouldReportTerminalState_whenArgumentIsAlsoInvalidForFailedInvoice() {
            Invoice invoice = failedInvoice();

            assertThatThrownBy(() -> invoice.markAsGenerated("   ", null))
                    .isInstanceOf(InvalidInvoiceStateException.class)
                    .hasMessageContaining("Invoice has been failed");
        }
    }

    // --------------------------------------------------------------- markAsFailed

    @Nested
    class MarkAsFailed {

        @Test
        void shouldRecordFailure() {
            Invoice invoice = processingInvoice();

            assertThat(invoice.markAsFailed(FAILURE_REASON, FAILED_AT)).isTrue();
            assertThat(invoice.getStatus()).isEqualTo(FAILED);
            assertThat(invoice.getFailureReason()).isEqualTo(FAILURE_REASON);
            assertThat(invoice.getFailedAt()).isEqualTo(FAILED_AT);
        }

        @Test
        void shouldTrimFailureReason_whenItHasSurroundingSpaces() {
            Invoice invoice = processingInvoice();

            invoice.markAsFailed("  " + FAILURE_REASON + "  ", FAILED_AT);

            assertThat(invoice.getFailureReason()).isEqualTo(FAILURE_REASON);
        }

        /** A coluna é `varchar(2000)`: estourar viraria violação de constraint no flush, não erro de negócio. */
        @Test
        void shouldTruncateFailureReason_whenItExceedsStoredLength() {
            Invoice invoice = processingInvoice();

            invoice.markAsFailed("x".repeat(MAX_FAILURE_REASON_LENGTH + 1), FAILED_AT);

            assertThat(invoice.getFailureReason()).hasSize(MAX_FAILURE_REASON_LENGTH);
        }

        @Test
        void shouldReportNoChange_whenFailureArrivesAgain() {
            Invoice invoice = failedInvoice();

            assertThat(invoice.markAsFailed(FAILURE_REASON, FAILED_AT)).isFalse();
        }

        /**
         * Idempotência por estado, nunca por texto: o motivo vem de {@code getMessage()} e é instável entre
         * ocorrências da mesma falha. Primeira falha vence, e a redelivery não reescreve o registro.
         */
        @Test
        void shouldKeepFirstReason_whenDifferentFailureArrivesAgain() {
            Invoice invoice = failedInvoice();

            assertThat(invoice.markAsFailed(OTHER_FAILURE_REASON, OTHER_FAILED_AT)).isFalse();
            assertThat(invoice.getFailureReason()).isEqualTo(FAILURE_REASON);
            assertThat(invoice.getFailedAt()).isEqualTo(FAILED_AT);
        }

        /** O PDF já está no bucket: marcar a nota como falha apagaria um fato consumado. */
        @Test
        void shouldRejectFailure_whenInvoiceWasGenerated() {
            Invoice invoice = generatedInvoice();

            assertThatThrownBy(() -> invoice.markAsFailed(FAILURE_REASON, FAILED_AT))
                    .isInstanceOf(IllegalInvoiceStatusChangeException.class)
                    .hasMessageContaining("GENERATED invoice cannot be marked as FAILED");

            assertThat(invoice.getStatus()).isEqualTo(GENERATED);
            assertThat(invoice.getFailureReason()).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void shouldRejectFailure_whenReasonIsBlank(String blankReason) {
            Invoice invoice = processingInvoice();

            assertThatThrownBy(() -> invoice.markAsFailed(blankReason, FAILED_AT))
                    .isInstanceOf(InvalidInvoiceException.class)
                    .hasMessageContaining("Failure reason is required");

            assertThat(invoice.getStatus()).isEqualTo(PROCESSING);
        }

        @Test
        void shouldRejectFailure_whenFailedDateIsMissing() {
            Invoice invoice = processingInvoice();

            assertThatThrownBy(() -> invoice.markAsFailed(FAILURE_REASON, null))
                    .isInstanceOf(InvalidInvoiceException.class)
                    .hasMessageContaining("Failed at date is required");

            assertThat(invoice.getStatus()).isEqualTo(PROCESSING);
        }

        /** Argumento é validado antes das guardas de estado, então motivo em branco é recusado mesmo após a falha. */
        @Test
        void shouldRejectFailure_whenReasonIsBlankAfterFailure() {
            Invoice invoice = failedInvoice();

            assertThatThrownBy(() -> invoice.markAsFailed("   ", FAILED_AT))
                    .isInstanceOf(InvalidInvoiceException.class)
                    .hasMessageContaining("Failure reason is required");
        }
    }

    // ------------------------------------------------------------------ rehydrate

    @Nested
    class Rehydrate {

        @Test
        void shouldRestoreProcessingInvoice() {
            Invoice invoice = Invoice.rehydrate(INVOICE_ID, ORDER_ID, PROCESSING, null, null, null, null);

            assertThat(invoice.getStatus()).isEqualTo(PROCESSING);
            assertThat(invoice.getStorageKey()).isNull();
            assertThat(invoice.getGeneratedAt()).isNull();
        }

        @Test
        void shouldRestoreGeneratedInvoice() {
            Invoice invoice = storedGeneratedInvoice();

            assertThat(invoice.getStatus()).isEqualTo(GENERATED);
            assertThat(invoice.getStorageKey()).isEqualTo(STORAGE_KEY);
            assertThat(invoice.getGeneratedAt()).isEqualTo(GENERATED_AT);
        }

        @Test
        void shouldRestoreFailedInvoice() {
            Invoice invoice = storedFailedInvoice();

            assertThat(invoice.getStatus()).isEqualTo(FAILED);
            assertThat(invoice.getFailureReason()).isEqualTo(FAILURE_REASON);
            assertThat(invoice.getFailedAt()).isEqualTo(FAILED_AT);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidStoredIdentity")
        void shouldRejectRehydration_whenIdentityIsInvalid(
                String label,
                UUID id,
                Long orderId,
                InvoiceStatus status,
                String expectedMessage
        ) {
            assertThatThrownBy(() -> Invoice.rehydrate(id, orderId, status, null, null, null, null))
                    .isInstanceOf(InvoiceRehydrationException.class)
                    .hasMessageContaining(expectedMessage);
        }

        static Stream<Arguments> invalidStoredIdentity() {
            return Stream.of(
                    arguments("null invoice ID", null, ORDER_ID, PROCESSING, "must have an ID"),
                    arguments("null status", INVOICE_ID, ORDER_ID, null, "must have a status"),
                    arguments("null order ID", INVOICE_ID, null, PROCESSING, "Order ID must not be null"),
                    arguments("order ID zero", INVOICE_ID, 0L, PROCESSING, "Order ID must be greater than zero")
            );
        }

        // --- Não alcançou o marco: proíbe os campos

        @ParameterizedTest(name = "{0}")
        @MethodSource("processingCarryingData")
        void shouldRejectRehydration_whenProcessingInvoiceCarriesData(
                String label,
                String storageKey,
                Instant generatedAt,
                Instant failedAt,
                String failureReason
        ) {
            assertThatThrownBy(() -> Invoice.rehydrate(
                    INVOICE_ID, ORDER_ID, PROCESSING, storageKey, generatedAt, failedAt, failureReason))
                    .isInstanceOf(InvoiceRehydrationException.class)
                    .hasMessageContaining("must not contain generation or failure data");
        }

        static Stream<Arguments> processingCarryingData() {
            return Stream.of(
                    arguments("storage key", STORAGE_KEY, null, null, null),
                    arguments("generated date", null, GENERATED_AT, null, null),
                    arguments("failed date", null, null, FAILED_AT, null),
                    arguments("failure reason", null, null, null, FAILURE_REASON)
            );
        }

        @Test
        void shouldRejectRehydration_whenGeneratedInvoiceCarriesFailureData() {
            assertThatThrownBy(() -> Invoice.rehydrate(
                    INVOICE_ID, ORDER_ID, GENERATED, STORAGE_KEY, GENERATED_AT, FAILED_AT, FAILURE_REASON))
                    .isInstanceOf(InvoiceRehydrationException.class)
                    .hasMessageContaining("must not contain failure data");
        }

        @Test
        void shouldRejectRehydration_whenFailedInvoiceCarriesGenerationData() {
            assertThatThrownBy(() -> Invoice.rehydrate(
                    INVOICE_ID, ORDER_ID, FAILED, STORAGE_KEY, GENERATED_AT, FAILED_AT, FAILURE_REASON))
                    .isInstanceOf(InvoiceRehydrationException.class)
                    .hasMessageContaining("must not contain storage key or generated date");
        }

        // --- Alcançou o marco: exige os campos

        @ParameterizedTest(name = "{0}")
        @MethodSource("incompleteGeneratedInvoice")
        void shouldRejectRehydration_whenGeneratedInvoiceIsIncomplete(
                String label,
                String storageKey,
                Instant generatedAt
        ) {
            assertThatThrownBy(() -> Invoice.rehydrate(
                    INVOICE_ID, ORDER_ID, GENERATED, storageKey, generatedAt, null, null))
                    .isInstanceOf(InvoiceRehydrationException.class)
                    .hasMessageContaining("must contain storage key and generated date");
        }

        static Stream<Arguments> incompleteGeneratedInvoice() {
            return Stream.of(
                    arguments("no storage key", null, GENERATED_AT),
                    arguments("blank storage key", "   ", GENERATED_AT),
                    arguments("no generated date", STORAGE_KEY, null)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("incompleteFailedInvoice")
        void shouldRejectRehydration_whenFailedInvoiceIsIncomplete(
                String label,
                Instant failedAt,
                String failureReason
        ) {
            assertThatThrownBy(() -> Invoice.rehydrate(
                    INVOICE_ID, ORDER_ID, FAILED, null, null, failedAt, failureReason))
                    .isInstanceOf(InvoiceRehydrationException.class)
                    .hasMessageContaining("must contain failure reason and failure date");
        }

        static Stream<Arguments> incompleteFailedInvoice() {
            return Stream.of(
                    arguments("no failed date", null, FAILURE_REASON),
                    arguments("no failure reason", FAILED_AT, null),
                    arguments("blank failure reason", FAILED_AT, "   ")
            );
        }

        /**
         * Texto em branco é lido como ausente, e não como valor: uma coluna com espaços passa a violar a
         * coerência do mesmo jeito que uma coluna nula, que é o que os CHECKs com {@code btrim} já dizem.
         */
        @Test
        void shouldTreatBlankTextAsAbsent() {
            Invoice invoice = Invoice.rehydrate(INVOICE_ID, ORDER_ID, PROCESSING, "   ", null, null, "   ");

            assertThat(invoice.getStorageKey()).isNull();
            assertThat(invoice.getFailureReason()).isNull();
        }

        @Test
        void shouldNormalizeStoredText_whenItHasSurroundingSpaces() {
            Invoice invoice = Invoice.rehydrate(
                    INVOICE_ID, ORDER_ID, GENERATED, "  " + STORAGE_KEY + "  ", GENERATED_AT, null, null);

            assertThat(invoice.getStorageKey()).isEqualTo(STORAGE_KEY);
        }
    }

    // ----------------------------------------------------------------- identidade

    @Nested
    class Identity {

        /** O ID é cunhado pela aplicação antes de persistir, então serve de identidade desde a criação. */
        @Test
        void shouldEqualAnotherInvoice_whenIdsMatch() {
            assertThat(processingInvoice()).isEqualTo(Invoice.createNew(INVOICE_ID, ORDER_ID));
        }

        @Test
        void shouldNotEqualAnotherInvoice_whenIdsDiffer() {
            assertThat(processingInvoice()).isNotEqualTo(Invoice.createNew(OTHER_INVOICE_ID, ORDER_ID));
        }

        /** Diferente de `Order` e `Shipment`, aqui o hash pode ser o do ID: o UUIDv7 nunca muda no flush. */
        @Test
        void shouldHashById() {
            assertThat(processingInvoice()).hasSameHashCodeAs(Invoice.createNew(INVOICE_ID, ORDER_ID));
        }

        @Test
        void shouldKeepHashCode_whenStatusChanges() {
            Invoice invoice = processingInvoice();
            int beforeGeneration = invoice.hashCode();

            invoice.markAsGenerated(STORAGE_KEY, GENERATED_AT);

            assertThat(invoice.hashCode()).isEqualTo(beforeGeneration);
        }
    }

    // -------------------------------------------------------------------- helpers

    private static Invoice processingInvoice() {
        return Invoice.createNew(INVOICE_ID, ORDER_ID);
    }

    private static Invoice generatedInvoice() {
        Invoice invoice = processingInvoice();
        invoice.markAsGenerated(STORAGE_KEY, GENERATED_AT);

        return invoice;
    }

    private static Invoice failedInvoice() {
        Invoice invoice = processingInvoice();
        invoice.markAsFailed(FAILURE_REASON, FAILED_AT);

        return invoice;
    }

    private static Invoice storedGeneratedInvoice() {
        return Invoice.rehydrate(INVOICE_ID, ORDER_ID, GENERATED, STORAGE_KEY, GENERATED_AT, null, null);
    }

    private static Invoice storedFailedInvoice() {
        return Invoice.rehydrate(INVOICE_ID, ORDER_ID, FAILED, null, null, FAILED_AT, FAILURE_REASON);
    }
}
