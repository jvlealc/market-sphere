package io.github.jvlealc.marketsphere.orders.client.customers;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerClientService {

    private final CustomersClient customersClient;

    /**
     * Busca um cliente pelo ID.
     * Garante que o cliente está ativo e é válido.
     * Trata FeignException (404) e respostas 200 OK com body nulo.
     *
     * @param customerId ID do cliente.
     * @return {@code CustomerRepresentation} Os dados do cliente ativo.
     * @throws CustomerClientNotFoundException se o cliente não for encontrado.
     * */
    public CustomerRepresentation getCustomerById(Long customerId) {
        try {
            ResponseEntity<CustomerRepresentation> response = customersClient.getCustomerById(customerId);
            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("[Default] Customer service returned a null body (200 OK) for customerId: {}.", customerId);
                        return new CustomerClientNotFoundException("customerId", "Customer not found or returned an empty response for ID: " + customerId);
                    });
        } catch (FeignException.NotFound e) {
            log.error("[Default] Customer not found (404) via Feign client for customer ID: {}. Message: {}", customerId, e.getMessage());
            throw new CustomerClientNotFoundException("customerId", "Customer not found with ID: " + customerId);
        }
    }

    /**
     * Busca um cliente pelo ID. Esteja ele ativo ou inativo.
     * Trata FeignException (404) e respostas 200 OK com body nulo.
     *
     * @param customerId ID do cliente.
     * @return {@code CustomerRepresentation} Os dados do cliente (ativo ou inativo).
     * @throws CustomerClientNotFoundException se o cliente não for encontrado.
     * */
    public CustomerRepresentation getCustomerByIdIgnoringFilter(Long customerId) {
        try {
            ResponseEntity<CustomerRepresentation> response = customersClient.getCustomerByIdIgnoringFilter(customerId);
            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("[Ignoring Filter] Customer service returned a null body (200 OK) for customerId: {}.", customerId);
                        return new CustomerClientNotFoundException("customerId", "Customer not found or returned an empty response for ID: " + customerId);
                    });
        } catch (FeignException.NotFound e) {
            log.error("[Ignoring Filter] Customer not found (404) via Feign client for customer ID: {}. Message: {}", customerId, e.getMessage());
            throw new CustomerClientNotFoundException("customerId", "Customer not found with ID: " + customerId);
        }
    }
}
