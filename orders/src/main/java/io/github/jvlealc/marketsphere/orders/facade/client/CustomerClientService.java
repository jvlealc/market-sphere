package io.github.jvlealc.marketsphere.orders.facade.client;

import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.exception.client.customers.CustomerClientNotFoundException;


public interface CustomerClientService {

    /**
     * Busca um cliente pelo ID.
     * Garante que o cliente está ativo e é válido.
     * Trata FeignException (404) e respostas 200 OK com body nulo.
     *
     * @param customerId ID do cliente.
     * @return {@code CustomerRepresentation} Os dados do cliente ativo.
     * @throws CustomerClientNotFoundException se o cliente não for encontrado.
     * */
    CustomerRepresentation getCustomerById(Long customerId);

    /**
     * Busca um cliente pelo ID. Esteja ele ativo ou inativo.
     * Trata FeignException (404) e respostas 200 OK com body nulo.
     *
     * @param customerId ID do cliente.
     * @return {@code CustomerRepresentation} Os dados do cliente (ativo ou inativo).
     * @throws CustomerClientNotFoundException se o cliente não for encontrado.
     * */
    CustomerRepresentation getCustomerByIdIgnoringFilter(Long customerId);
}
