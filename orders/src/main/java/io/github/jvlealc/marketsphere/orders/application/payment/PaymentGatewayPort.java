package io.github.jvlealc.marketsphere.orders.application.payment;

public interface PaymentGatewayPort {

    /**
     * @param idempotencyKey repetir a chamada com a mesma chave não pode gerar uma segunda cobrança.
     *                       O relay reentrega a mesma mensagem de outbox depois de qualquer falha
     */
    PaymentRequestReceipt requestPayment(Long orderId, String idempotencyKey);
}
