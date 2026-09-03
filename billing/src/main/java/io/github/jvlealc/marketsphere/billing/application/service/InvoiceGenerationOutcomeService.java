package io.github.jvlealc.marketsphere.billing.application.service;

import io.github.jvlealc.marketsphere.billing.application.exception.InvoiceNotFoundException;
import io.github.jvlealc.marketsphere.billing.application.factory.OrderBilledOutboxMessageFactory;
import io.github.jvlealc.marketsphere.billing.application.model.messaging.EventLineage;
import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidCustomer;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceRepositoryPort;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OutboxRepositoryPort;
import io.github.jvlealc.marketsphere.billing.domain.model.Invoice;
import io.github.jvlealc.marketsphere.billing.domain.model.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Concentra os dois desfechos da geração de nota fiscal, cada um em sua própria transação.
 * <p>
 * Tudo que toca a rede — renderização do PDF e armazenamento do objeto — acontece <em>antes</em> de estes
 * métodos serem chamados. Uma transação aqui retém uma conexão do pool, e reter conexão durante I/O remoto
 * é como se esgota um HikariCP antes de qualquer outro recurso do sistema ceder.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceGenerationOutcomeService {

    private static final String UNKNOWN_FAILURE_REASON = "No error message provided";

    private final InvoiceRepositoryPort invoiceRepository;
    private final OutboxRepositoryPort outboxRepository;
    private final OrderBilledOutboxMessageFactory outboxFactory;

    /**
     * Promove a nota a {@code GENERATED} e enfileira {@code ORDER_BILLED} nos dois canais na mesma transação
     * da mudança de estado — essa atomicidade é a razão de existir do padrão outbox.
     */
    @Transactional
    public void confirmGeneration(
            UUID invoiceId,
            OrderPaidCustomer customer,
            String storageKey,
            Instant generatedAt,
            EventLineage eventLineage
    ) {
        Objects.requireNonNull(invoiceId, "invoiceId must not be null");
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(eventLineage, "eventLineage must not be null");

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (invoice.getStatus() != InvoiceStatus.PROCESSING) {
            log.info("Invoice {} is already {}. Another worker won the race; nothing to persist.",
                    invoiceId, invoice.getStatus());
            return;
        }

        invoice.markAsGenerated(storageKey, generatedAt);

        invoiceRepository.save(invoice);
        outboxRepository.appendNew(outboxFactory.createForMessaging(invoice, customer, eventLineage));
        outboxRepository.appendNew(outboxFactory.createForEmail(invoice, customer, eventLineage));

        log.info("Invoice {} for order {} marked as GENERATED and ORDER_BILLED enqueued on both channels.",
                invoice.getId(), invoice.getOrderId());
    }

    /**
     * Registra uma falha de <strong>negócio</strong> — aquela em que reprocessar o mesmo evento não muda o
     * resultado. {@code FAILED} é terminal: {@link Invoice#markAsGenerated} se recusa a rodar depois, então
     * uma falha transitória de infraestrutura jamais pode chegar aqui. Ver a regra de classificação em
     * {@code HandleOrderPaidUseCase}.
     */
    @Transactional
    public void recordTerminalFailure(UUID invoiceId, Throwable cause, Instant failedAt) {
        Objects.requireNonNull(invoiceId, "invoiceId must not be null");

        Optional<Invoice> foundInvoice = invoiceRepository.findById(invoiceId);

        if (foundInvoice.isEmpty()) {
            log.warn("Invoice {} was not found while recording a generation failure.", invoiceId);
            return;
        }

        Invoice invoice = foundInvoice.get();

        if (invoice.getStatus() != InvoiceStatus.PROCESSING) {
            log.warn("Invoice {} is {} and cannot be marked as FAILED.", invoiceId, invoice.getStatus());
            return;
        }

        if (!invoice.markAsFailed(describeFailure(cause), failedAt)) {
            log.info("Invoice {} had already been marked as FAILED. Keeping the original reason.", invoiceId);
            return;
        }

        invoiceRepository.save(invoice);

        log.warn("Invoice {} for order {} marked as FAILED.", invoice.getId(), invoice.getOrderId());
    }

    private static String describeFailure(Throwable cause) {
        if (cause == null) {
            return UNKNOWN_FAILURE_REASON;
        }

        String exceptionName = cause.getClass().getSimpleName();
        String message = cause.getMessage();

        return message == null || message.isBlank()
                ? exceptionName
                : exceptionName + ": " + message;
    }
}
