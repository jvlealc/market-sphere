package io.github.jvlealc.marketsphere.customers.service;

import io.github.jvlealc.marketsphere.customers.dto.CustomerRequestDto;
import io.github.jvlealc.marketsphere.customers.dto.CustomerResponseDto;

import java.util.List;

public interface CustomerService {

    CustomerResponseDto createCustomer(CustomerRequestDto customerRequestDto);
    CustomerResponseDto getCustomerById(Long id);
    List<CustomerResponseDto> getAllCustomers();
    void updateCustomer(Long customerId, CustomerRequestDto customerRequestDto);

    /**
     * Realiza a exclusão lógica de um cliente
     * @param customerId ID do cliente a ser inativado
     * */
    void deleteCustomerById(Long customerId);

    /**
     * Reativa um cliente que foi logicamente excluído.
     * @param customerId O ID do cliente a ser reativado.
     */
    void reactivateCustomerById(Long customerId);

    /**
     * Busca um cliente pelo seu ID, esteja ele <strong>ativo</strong> ou <strong>inativo</strong>
     * @param customerId ID do cliente
     */
    CustomerResponseDto getCustomerByIdIgnoringFilter(Long customerId);
}
