package io.github.jvlealc.marketsphere.billing.subscriber.mapper;

import io.github.jvlealc.marketsphere.billing.model.Address;
import io.github.jvlealc.marketsphere.billing.model.Customer;
import io.github.jvlealc.marketsphere.billing.model.Order;
import io.github.jvlealc.marketsphere.billing.model.OrderItem;
import io.github.jvlealc.marketsphere.billing.subscriber.event.OrderPaidEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;

    public OrderMapper(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    /**
     * Converte a representação de um pedido completo (vinda do Kafka)
     * para o modelo de domínio interno.
     *
     * @param orderPaidEvent representação do pedido recebida via Kafka
     * @return Order - objeto de domínio correspondente
     */
    public Order toDomainModel(OrderPaidEvent orderPaidEvent) {
        Customer customer = new Customer(
                orderPaidEvent.customer().fullName(),
                orderPaidEvent.customer().nationalId(),
                orderPaidEvent.customer().email(),
                orderPaidEvent.customer().phoneNumber(),
                new Address(
                        orderPaidEvent.customer().postalCode(),
                        orderPaidEvent.customer().street(),
                        orderPaidEvent.customer().number(),
                        orderPaidEvent.customer().complement(),
                        orderPaidEvent.customer().neighborhood(),
                        orderPaidEvent.customer().city(),
                        orderPaidEvent.customer().state(),
                        orderPaidEvent.customer().country()
                )
        );

        List<OrderItem> orderItems = orderPaidEvent.orderItems()
                .stream()
                .map(orderItemMapper::toDomainModel)
                .toList();

        return new Order(
                orderPaidEvent.orderId(),
                orderPaidEvent.orderDate(),
                orderPaidEvent.orderObservations(),
                customer,
                orderItems,
                orderPaidEvent.orderTotal()
        );
    }
}
