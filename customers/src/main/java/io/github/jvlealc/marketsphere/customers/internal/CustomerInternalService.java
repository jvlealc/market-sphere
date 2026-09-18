package io.github.jvlealc.marketsphere.customers.internal;

import io.github.jvlealc.marketsphere.customers.dto.CustomerResponse;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNotFoundException;
import io.github.jvlealc.marketsphere.customers.mapper.CustomerRestMapper;
import io.github.jvlealc.marketsphere.customers.shared.rest.pagination.PageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CustomerInternalService {

    private final CustomerInternalReadRepository customerRepository;
    private final CustomerRestMapper customerMapper;

    @Transactional(readOnly = true)
    PageModel<CustomerResponse> getAllCustomers(int pageNumber, int pageSize) {
        return customerMapper.toPageModel(
                customerRepository.findAll(PageRequest.of(pageNumber, pageSize))
        );
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
