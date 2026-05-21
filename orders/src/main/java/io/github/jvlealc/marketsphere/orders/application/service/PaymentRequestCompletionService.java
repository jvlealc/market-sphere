package io.github.jvlealc.marketsphere.orders.application.service;

import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.payment.PaymentRequestReceipt;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentRequestCompletionService {

    private final OrderRepositoryPort orderRepository;
    private final OutboxRepositoryPort outboxRepository;

    @Transactional
    public void complete(Long orderId, PaymentRequestReceipt receipt, OutboxMessage paymentRequestMessage) {
        //1 buscar pedido via banco
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        //2 Registrar no domínio que a solicitação de pagamento foi criada
        order.registerPaymentRequest(receipt.paymentKey());
        orderRepository.save(order);

        //3 Marcar a mensagem original PAYMENT_REQUEST_REQUIRED como PROCESSED.
        outboxRepository.markAsProcessed(paymentRequestMessage.getId());
    }
}
