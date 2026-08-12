package io.github.jvlealc.marketsphere.orders.infrastructure.client.mockpayment;

import io.github.jvlealc.marketsphere.orders.application.ports.out.payment.PaymentGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.payment.PaymentRequestReceipt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MockPaymentClientAdapter implements PaymentGatewayPort {

    /**
     * Simula uma solicitação de pagamento a um gateway bancário.
     * Retorna uma resposta mockada com:
     * - String aleatória como chave de pagamento
     * - HTTP successful 200 OK
     * - Mensagem de sucesso genérica
     * - Timestamp atual
     *
     * @param orderId ID do pedido a ser pago
     * @return {@link MockPaymentRepresentation} contendo chave de pagamento, status, mensagem e timestamp
     */
    @Override
    public PaymentRequestReceipt requestPayment(Long orderId, String idempotencyKey) {
        log.info("Requesting payment for order ID {} with idempotency-key {}.", orderId, idempotencyKey);
        return toApplicationModel(MockPaymentRepresentation.mock(orderId, idempotencyKey));
    }

    private static PaymentRequestReceipt toApplicationModel(MockPaymentRepresentation representation) {
        return new PaymentRequestReceipt(
                representation.paymentKey(),
                representation.statusCode(),
                representation.message(),
                representation.requestedAt()
        );
    }
}
