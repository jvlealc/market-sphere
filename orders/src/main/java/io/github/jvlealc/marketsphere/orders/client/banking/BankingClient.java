package io.github.jvlealc.marketsphere.orders.client.banking;

import io.github.jvlealc.marketsphere.orders.client.banking.representation.BankingPaymentRepresentation;

public interface BankingClient {

    /**
     * Simula uma solicitação de pagamento a um gateway bancário.
     *
     * @param orderId ID do pedido a ser pago
     * @return representação contendo chave de pagamento, status, mensagem e timestamp
     * */
    BankingPaymentRepresentation requestPayment(Long orderId);
}
