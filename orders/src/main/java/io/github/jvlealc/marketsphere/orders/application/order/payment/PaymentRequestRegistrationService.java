package io.github.jvlealc.marketsphere.orders.application.order.payment;

import io.github.jvlealc.marketsphere.orders.application.order.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.payment.PaymentRequestReceipt;
import io.github.jvlealc.marketsphere.orders.application.order.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentRequestRegistrationService {

    private final OrderRepositoryPort orderRepository;

    /**
     * Registra no pedido a chave devolvida pelo gateway.
     *
     * <p>Não conclui a mensagem de outbox: quem detém o lease é o relay, e só ele tem o
     * {@code lockToken} que as conclusões exigem. Uma reentrega antes da conclusão repete esta
     * gravação, o que é inócuo porque a chave vem de uma chamada idempotente ao gateway.
     */
    @Transactional
    public void registerPaymentRequest(Long orderId, PaymentRequestReceipt receipt) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        boolean isRegistered = order.registerPaymentRequest(receipt.paymentKey());

        if (isRegistered) {
            orderRepository.save(order);
        }
    }
}
