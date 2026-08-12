package io.github.jvlealc.marketsphere.orders.application.service;

import io.github.jvlealc.marketsphere.orders.application.factory.OrderOutboxMessageFactory;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OutboxRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
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
