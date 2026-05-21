package io.github.jvlealc.marketsphere.customers.service;

import io.github.jvlealc.marketsphere.customers.client.BrasilApiClient;
import io.github.jvlealc.marketsphere.customers.client.BrasilApiAddressRepresentation;
import io.github.jvlealc.marketsphere.customers.dto.CustomerRequestDto;
import io.github.jvlealc.marketsphere.customers.dto.CustomerResponseDto;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNotFoundException;
import io.github.jvlealc.marketsphere.customers.mapper.CustomerMapper;
import io.github.jvlealc.marketsphere.customers.model.Customer;
import io.github.jvlealc.marketsphere.customers.repository.CustomerRepository;
import io.github.jvlealc.marketsphere.customers.validator.CustomerValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final BrasilApiClient brasilApiClient;
    private final CustomerMapper mapper;
    private final CustomerValidator validator;

    @Transactional
    public CustomerResponseDto createCustomer(final CustomerRequestDto customerRequestDto) {
        validator.validateForCreate(customerRequestDto);
        BrasilApiAddressRepresentation brasilApiAddressRepresentation = brasilApiClient.getAddressByPostalCode(customerRequestDto.postalCode());
        return mapper.toCustomerDto(
                repository.save(mapper.toCustomerEntity(customerRequestDto, brasilApiAddressRepresentation))
        );
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(final Long customerId) {
        return repository.findById(customerId)
                .map(mapper::toCustomerDto)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers() {
        return repository.findAll()
                .stream()
                .map(mapper::toCustomerDto)
                .toList();
    }

    @Transactional
    public void updateCustomer(final Long customerId, final CustomerRequestDto customerRequestDto) {
        Customer customerToUpdate = repository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        validator.validateForUpdate(customerToUpdate, customerRequestDto);
        // valida postalCode chamando Brasil API e utiliza os dados de endereço retornados para persistência
        BrasilApiAddressRepresentation brasilApiAddressRepresentation = brasilApiClient.getAddressByPostalCode(customerRequestDto.postalCode());
        mapper.updateCustomerEntity(customerToUpdate, customerRequestDto, brasilApiAddressRepresentation);
    }

    /**
     * Realiza a exclusão lógica de um cliente
     * @param customerId ID do cliente a ser inativado
     * */
    @Transactional
    public void deleteCustomerById(final Long customerId) {
        if (!repository.existsById(customerId)) {
            throw new  CustomerNotFoundException(customerId);
        }
        repository.deleteById(customerId);
    }

    /**
     * Reativa um cliente que foi logicamente excluído.
     * @param customerId O ID do cliente a ser reativado.
     */
    @Transactional
    public void reactivateCustomerById(Long customerId) {
        Customer customerToReactivate = repository.findInactiveById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Inactive product with ID " + customerId + " not found."));
        customerToReactivate.setActive(true);
    }

    /**
     * Busca um cliente pelo seu ID, esteja ele <strong>ativo</strong> ou <strong>inativo</strong>
     * @param customerId ID do cliente
     */
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerByIdIncludingInactive(Long customerId) {
        return mapper.toCustomerDto(
                repository.findByIdIncludingInactive(customerId)
                        .orElseThrow( () -> new CustomerNotFoundException(customerId))
        );
    }
}
