package io.github.jvlealc.marketsphere.customers.validator;

import io.github.jvlealc.marketsphere.customers.dto.CustomerRequest;
import io.github.jvlealc.marketsphere.customers.exception.CustomerEmailAlreadyInUseException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNationalIdAlreadyInUseException;
import io.github.jvlealc.marketsphere.customers.model.Customer;
import io.github.jvlealc.marketsphere.customers.repository.CustomerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerValidator {

    private final CustomerJpaRepository repository;

    /**
     * Valida um novo request antes de criar.
     */
    public void validateForCreate(CustomerRequest request) {
        if (isEmailAlreadyInUse(request.email())) {
            throw new CustomerEmailAlreadyInUseException(request.email());
        }

        if (isNationalIdAlreadyInUse(request.nationalId())) {
            throw new CustomerNationalIdAlreadyInUseException(request.nationalId());
        }
    }

    /**
     * Valida um customer existente antes de atualizar.
     */
    public void validateForUpdate(Customer existingCustomer, CustomerRequest request) {
        // valida email duplicado, somente se alterou
        if (!existingCustomer.getEmail().equals(request.email()) && isEmailAlreadyInUse(request.email())) {
            throw new CustomerEmailAlreadyInUseException(request.email());
        }

        // valida nationalId duplicado, somente se alterou
        if (!existingCustomer.getNationalId().equals(request.nationalId())
                && isNationalIdAlreadyInUse(request.nationalId()))
        {
            throw new CustomerNationalIdAlreadyInUseException(request.nationalId());
        }
    }

    private boolean isEmailAlreadyInUse(String customerEmail) {
        return repository.existsByEmailIncludingInactive(customerEmail);
    }

    private boolean isNationalIdAlreadyInUse(String customerNationalId) {
        return repository.existsByNationalIdIncludingInactive(customerNationalId);
    }
}
