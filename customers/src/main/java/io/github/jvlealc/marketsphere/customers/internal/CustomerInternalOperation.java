package io.github.jvlealc.marketsphere.customers.internal;

import io.github.jvlealc.marketsphere.customers.dto.CustomerResponse;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNotFoundException;
import io.github.jvlealc.marketsphere.customers.mapper.CustomerRestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CustomerInternalOperation {

    private final CustomerInternalJpaRepository customerRepository;
    private final CustomerRestMapper customerMapper;

    @Transactional(readOnly = true)
    Page<CustomerResponse> getAllCustomers(int page, int size) {
        return customerRepository.findAll(PageRequest.of(page, size))
                .map(customerMapper::toResponse);
    }

    /**
     * Busca um cliente pelo seu ID, esteja ele <strong>ativo</strong> ou <strong>inativo</strong>
     * @param customerId ID do cliente
     */
    @Transactional(readOnly = true)
    CustomerResponse getCustomerByIdIncludingInactive(Long customerId) {
        return customerMapper.toResponse(
                customerRepository.findByIdIncludingInactive(customerId)
                        .orElseThrow( () -> new CustomerNotFoundException(customerId))
        );
    }
}
