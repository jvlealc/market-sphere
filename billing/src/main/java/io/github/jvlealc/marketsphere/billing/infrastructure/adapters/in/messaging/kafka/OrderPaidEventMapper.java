package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.messaging.kafka;

import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidAddress;
import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidCustomer;
import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidItem;
import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class OrderPaidEventMapper {

    /**
     * Converte a representação de um pedido completo (vinda do Kafka)
     * para o modelo de aplicação interno.
     *
     * @param orderPaidEvent representação do pedido recebida via Kafka
     * @return OrderPaidSnapshot - objeto da camada de aplicação correspondente
     */
    public OrderPaidSnapshot toApplicationModel(OrderPaidEvent orderPaidEvent) {
        OrderPaidCustomer customer = new OrderPaidCustomer(
                orderPaidEvent.customer().customerId(),
                orderPaidEvent.customer().fullName(),
                orderPaidEvent.customer().nationalId(),
                orderPaidEvent.customer().email(),
                orderPaidEvent.customer().phoneNumber(),
                new OrderPaidAddress(
                        orderPaidEvent.customer().postalCode(),
                        orderPaidEvent.customer().street(),
                        orderPaidEvent.customer().houseNumber(),
                        orderPaidEvent.customer().complement(),
                        orderPaidEvent.customer().neighborhood(),
                        orderPaidEvent.customer().city(),
                        orderPaidEvent.customer().state(),
                        orderPaidEvent.customer().country()
                )
        );

        List<OrderPaidItem> orderItems = orderPaidEvent.orderItems()
                .stream()
                .map(OrderPaidEventMapper::toApplicationModel)
                .toList();

        return new OrderPaidSnapshot(
                orderPaidEvent.orderId(),
                orderPaidEvent.orderDate(),
                orderPaidEvent.orderObservations(),
                customer,
                orderItems,
                orderPaidEvent.orderTotal()
        );
    }

    /**
     * Converte a representação de um item de pedido (vinda do Kafka)
     * para o modelo de aplicação.
     *
     * @param orderPaidItemPayload representação do item do pedido recebida via Kafka
     * @return OrderPaidItem - objeto da camada de aplicação correspondente
     */
    private static OrderPaidItem toApplicationModel(OrderPaidItemPayload orderPaidItemPayload) {
        return new OrderPaidItem(
                orderPaidItemPayload.productId(),
                orderPaidItemPayload.productName(),
                orderPaidItemPayload.unitPrice(),
                orderPaidItemPayload.amount()
        );
    }
}
