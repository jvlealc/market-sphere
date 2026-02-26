package io.github.jvlealc.marketsphere.billing.subscriber.mapper;

import io.github.jvlealc.marketsphere.billing.model.OrderItem;
import io.github.jvlealc.marketsphere.billing.subscriber.event.OrderItemPayload;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    /**
     * Converte a representação de um item de pedido (vinda do Kafka)
     * para o modelo de domínio interno.
     *
     * @param orderItemPayload representação do item do pedido recebida via Kafka
     * @return OrderItem - objeto de domínio correspondente
     */
    public OrderItem toDomainModel(OrderItemPayload orderItemPayload) {
        return new OrderItem(
                orderItemPayload.productId(),
                orderItemPayload.productName(),
                orderItemPayload.unitPrice(),
                orderItemPayload.amount()
        );
    }
}
