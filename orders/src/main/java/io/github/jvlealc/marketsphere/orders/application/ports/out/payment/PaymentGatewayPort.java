package io.github.jvlealc.marketsphere.orders.application.ports.out.payment;

public interface PaymentGatewayPort {

    /**
     * Simula uma solicitação de pagamento de um pedido a um gateway bancário.
     *
     * @param orderId ID do pedido a ser pago
     * @param idempotencyKey chave usada para evitar solicitações duplicadas em retentativas
     * @return representação contendo chave de pagamento, status, mensagem e timestamp
     * */
    PaymentRequestReceipt requestPayment(Long orderId, String idempotencyKey);
}
