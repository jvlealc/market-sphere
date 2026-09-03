package io.github.jvlealc.marketsphere.billing.application.model.outbox.payload;

import java.util.UUID;

/**
 * Payload de {@code ORDER_BILLED} no canal {@code EMAIL}.
 * <p>
 * O nome acompanha o canal, como o irmão {@link OrderBilledMessagingPayload} — o enum persistido em
 * {@code outbox_messages.channel} é quem fixa o vocabulário. Os nomes dos campos JSON não mudaram com o
 * rename da classe, então o contrato gravado não foi afetado.
 */
public record OrderBilledEmailPayload(
        Long orderId,
        UUID invoiceId,
        String storageKey,
        String customerEmail,
        String customerName
) implements OrderBilledPayload {

    public OrderBilledEmailPayload {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must not be null and must be greater than zero");
        }

        if (invoiceId == null) {
            throw new IllegalArgumentException("invoiceId must not be null");
        }

        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey must not be null or blank");
        }

        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("customerEmail must not be null or blank");
        }

        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("customerName must not be null or blank");
        }

        storageKey = storageKey.trim();
        customerEmail = customerEmail.trim();
        customerName = customerName.trim();
    }
}
