package io.github.jvlealc.marketsphere.orders.client.banking;

import io.github.jvlealc.marketsphere.orders.client.banking.representation.BankingPaymentRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class MockBankingClient implements BankingClient {

    /**
     * Simula uma solicitação de pagamento a um gateway bancário.
     * Retorna uma resposta mockada com:
     * - String aleatória como chave de pagamento
     * - HTTP successful 200 OK
     * - Mensagem de sucesso genérica
     * - Timestamp atual
     *
     * @param orderId ID do pedido a ser pago
     * @return {@link BankingPaymentRepresentation} contendo chave de pagamento, status, mensagem e timestamp
     */
    @Override
    public BankingPaymentRepresentation requestPayment(Long orderId) {
        log.info("Requesting payment for order ID {}.", orderId);

        return new BankingPaymentRepresentation(
                UUID.randomUUID().toString(),
                HttpStatus.OK.value(),
                "Payment request simulated successfully for order ID: " + orderId,
                Instant.now()
        );
    }
}
