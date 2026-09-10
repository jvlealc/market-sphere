package io.github.jvlealc.marketsphere.customers.service;

import io.github.jvlealc.marketsphere.customers.dto.CustomerRequest;
import io.github.jvlealc.marketsphere.customers.dto.CustomerResponse;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNotFoundException;
import io.github.jvlealc.marketsphere.customers.mapper.CustomerRestMapper;
import io.github.jvlealc.marketsphere.customers.model.Customer;
import io.github.jvlealc.marketsphere.customers.repository.CustomerJpaRepository;
import io.github.jvlealc.marketsphere.customers.validator.CustomerValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerJpaRepository customerRepository;
    private final CustomerRestMapper customerMapper;
    private final CustomerValidator customerValidator;

    @Transactional
    public Long createCustomer(CustomerRequest request) {
        customerValidator.validateForCreate(request);
        return customerRepository.save(
                customerMapper.toNewEntity(request)
        ).getId();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    @Transactional
    public void updateCustomer(Long customerId, CustomerRequest request) {
        Customer existingCustomer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        customerValidator.validateForUpdate(existingCustomer, request);

        existingCustomer.setFullName(request.fullName());
        existingCustomer.setNationalId(request.nationalId());
        existingCustomer.setEmail(request.email());
        existingCustomer.setPhoneNumber(request.phoneNumber());

        customerRepository.save(existingCustomer);
    }

    /**
     * Realiza a exclusão lógica de um cliente
     * @param customerId ID do cliente a ser inativado
     * */
    @Transactional
    public void deactivateCustomerById(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new  CustomerNotFoundException(customerId);
        }

        customerRepository.deleteById(customerId);
    }

    /**
     * Reativa um cliente que foi logicamente excluído.
     * @param customerId O ID do cliente a ser reativado.
     */
    @Transactional
    public void reactivateCustomerById(Long customerId) {
        Customer customerToReactivate = customerRepository.findInactiveById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Inactive customer with ID " + customerId + " not found."));
        customerToReactivate.setActive(true);
    }
}
