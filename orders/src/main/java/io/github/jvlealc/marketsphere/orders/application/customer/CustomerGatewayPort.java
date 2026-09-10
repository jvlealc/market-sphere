package io.github.jvlealc.marketsphere.orders.application.customer;

public interface CustomerGatewayPort {

    /**
     * Busca um cliente pelo ID.
     * Garante que o cliente está ativo e é válido.
     * Trata FeignException (404) e respostas 200 OK com body nulo.
     *
     * @param customerId ID do cliente.
     * @return {@code CustomerRepresentation} Os dados do cliente ativo.
     * @throws CustomerNotFoundException se o cliente não for encontrado.
     * */
    CustomerProfile getCustomerById(Long customerId);
}
