package io.github.jvlealc.marketsphere.orders.application.order.placement;

import io.github.jvlealc.marketsphere.orders.application.outbox.OrderOutboxMessageFactory;
import io.github.jvlealc.marketsphere.orders.application.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.order.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderPlacementService {

    private final OrderRepositoryPort orderRepository;
    private final OutboxRepositoryPort outboxRepository;
    private final OrderOutboxMessageFactory outboxFactory;

    /**
     * Persiste o pedido e a solicitação de pagamento na mesma transação.
     *
     * <p>Colaborador separado porque o caso de uso <strong>não</strong> pode ser transacional: ele faz
     * duas chamadas HTTP antes de chegar aqui, e uma transação aberta durante elas retém a conexão do
     * pool pelo tempo dos timeouts.
     */
    @Transactional
    public Long place(Order newOrder, EventLineage eventLineage) {
        Order savedOrder = orderRepository.save(newOrder);

        OutboxMessage paymentRequestMessage = outboxFactory.createForPaymentRequest(savedOrder, eventLineage);

        outboxRepository.appendNew(paymentRequestMessage);

        return savedOrder.getId();
    }
}
