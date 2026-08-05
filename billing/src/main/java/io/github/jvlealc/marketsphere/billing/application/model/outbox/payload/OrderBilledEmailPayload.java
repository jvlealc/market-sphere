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
            throw new IllegalArgumentException("Order ID is required and must be positive");
        }

        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID is required");
        }

        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key is required");
        }

        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Customer email is required");
        }

        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }

        storageKey = storageKey.trim();
        customerEmail = customerEmail.trim();
        customerName = customerName.trim();
    }
}
