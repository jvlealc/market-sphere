package io.github.jvlealc.marketsphere.billing.application.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * O pedido como estava no instante do pagamento — a única fonte de dados da nota fiscal.
 * <p>
 * {@code orderObservations} é opcional: a coluna {@code orders.observations} é nullable e o campo não
 * alimenta o documento — o {@code JasperInvoicePdfGeneratorAdapter} preenche
 * {@code ORDER_OBSERVATIONS} a partir do {@code MessageTranslator}, não do evento. Ver
 * {@link OrderPaidAddress} sobre o custo de validar mais que o contrato do emissor.
 */
public record OrderPaidSnapshot(
        Long orderId,
        Instant orderDate,
        String orderObservations,
        OrderPaidCustomer customer,
        List<OrderPaidItem> items,
        BigDecimal total
) {
    public OrderPaidSnapshot {
        if (orderId == null || orderId <= 0L) {
            throw new InvalidOrderPaidSnapshotException("orderId is required and must be positive");
        }

        if (orderDate == null) {
            throw new InvalidOrderPaidSnapshotException("orderDate is required");
        }

        orderObservations = orderObservations == null || orderObservations.isBlank()
                ? null
                : orderObservations.trim();

        if (customer == null) {
            throw new InvalidOrderPaidSnapshotException("customer is required");
        }

        if (items == null || items.isEmpty()) {
            throw new InvalidOrderPaidSnapshotException("items is required");
        }

        if (total == null || total.signum() <= 0) {
            throw new InvalidOrderPaidSnapshotException("total is required and must be positive");
        }
    }
}
