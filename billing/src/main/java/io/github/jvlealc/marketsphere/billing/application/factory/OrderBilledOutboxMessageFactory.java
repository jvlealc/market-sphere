package io.github.jvlealc.marketsphere.billing.application.factory;

import io.github.jvlealc.marketsphere.billing.application.model.messaging.EventLineage;
import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidCustomer;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxAggregateType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.payload.OrderBilledCustomerPayload;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.payload.OrderBilledMessagingPayload;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.payload.OrderBilledEmailPayload;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OutboxPayloadCodecPort;
import io.github.jvlealc.marketsphere.billing.domain.model.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Constrói as duas mensagens de outbox de {@code ORDER_BILLED}, uma por canal.
 * <p>
 * Ambas leem {@code storageKey} e {@code generatedAt} diretamente do agregado, então
 * {@link Invoice#markAsGenerated} precisa ter rodado antes — chamar qualquer um dos dois métodos sobre uma
 * nota ainda em {@code PROCESSING} produz campos nulos, e os construtores dos payloads rejeitam.
 * <p>
 * As chaves de idempotência são por canal: a mesma nota legitimamente gera uma mensagem para o Kafka e
 * outra para e-mail, e uma chave única faria a segunda colidir com a primeira.
 * <p>
 * As duas compartilham o mesmo {@link EventLineage} — é o mesmo fato de negócio saindo por dois caminhos,
 * então pertencem ao mesmo fluxo e têm a mesma causa.
 */
@Component
@RequiredArgsConstructor
public class OrderBilledOutboxMessageFactory {

    /**
     * Versão do contrato de {@code ORDER_BILLED}. Viaja no header {@code event-version} para que um
     * consumidor possa rotear por versão sem inspecionar o corpo.
     */
    private static final int ORDER_BILLED_EVENT_VERSION = 1;

    private final OutboxPayloadCodecPort payloadCodec;

    public OutboxMessage createForMessaging(Invoice invoice, OrderPaidCustomer customer, EventLineage eventLineage) {
        OrderBilledMessagingPayload payload = new OrderBilledMessagingPayload(
                invoice.getOrderId(),
                invoice.getId(),
                invoice.getGeneratedAt(),
                new OrderBilledCustomerPayload(
                        customer.customerId(),
                        customer.fullName(),
                        customer.email()
                )
        );

        return OutboxMessage.createNew(
                OutboxAggregateType.INVOICE,
                invoice.getId().toString(),
                OutboxEventType.ORDER_BILLED,
                ORDER_BILLED_EVENT_VERSION,
                invoice.getGeneratedAt(),
                OutboxChannel.MESSAGING,
                partitionKeyOf(invoice),
                payloadCodec.serialize(payload),
                messagingIdempotencyKey(invoice.getId()),
                eventLineage
        );
    }

    public OutboxMessage createForEmail(Invoice invoice, OrderPaidCustomer customer, EventLineage eventLineage) {
        OrderBilledEmailPayload payload = new OrderBilledEmailPayload(
                invoice.getOrderId(),
                invoice.getId(),
                invoice.getStorageKey(),
                customer.email(),
                customer.fullName()
        );

        return OutboxMessage.createNew(
                OutboxAggregateType.INVOICE,
                invoice.getId().toString(),
                OutboxEventType.ORDER_BILLED,
                ORDER_BILLED_EVENT_VERSION,
                invoice.getGeneratedAt(),
                OutboxChannel.EMAIL,
                null,
                payloadCodec.serialize(payload),
                emailIdempotencyKey(invoice.getId()),
                eventLineage
        );
    }

    /**
     * O {@code orderId}, e não o {@code invoiceId}: a chave de partição precisa ser a chave de negócio pela
     * qual os eventos de um pedido se ordenam entre si. Usar a identidade do agregado espalharia eventos do
     * mesmo pedido por partições diferentes assim que houvesse um segundo tipo de evento de faturamento.
     */
    private static String partitionKeyOf(Invoice invoice) {
        return invoice.getOrderId().toString();
    }

    private static String messagingIdempotencyKey(UUID invoiceId) {
        return "messaging-order-billed-invoice-%s".formatted(invoiceId);
    }

    private static String emailIdempotencyKey(UUID invoiceId) {
        return "email-order-billed-invoice-%s".formatted(invoiceId);
    }
}
