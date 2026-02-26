package io.github.jvlealc.marketsphere.customers.validator;

import io.github.jvlealc.marketsphere.customers.dto.CustomerRequestDto;
import io.github.jvlealc.marketsphere.customers.exception.CustomerEmailAlreadyInUseException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNationalIdAlreadyInUseException;
import io.github.jvlealc.marketsphere.customers.model.Customer;
import io.github.jvlealc.marketsphere.customers.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerValidator {

    private final CustomerRepository repository;

    /**
     * Valida um novo customer antes de criar.
     */
    public void validateForCreate(final CustomerRequestDto customerRequestDto) {
        log.info("Validating CustomerRequestDto for create: {}", customerRequestDto);
        if (isEmailAlreadyInUse(customerRequestDto.email())) {
            throw new CustomerEmailAlreadyInUseException(customerRequestDto.email());
        }
        if (isNationalIdAlreadyInUse(customerRequestDto.nationalId())) {
            throw new CustomerNationalIdAlreadyInUseException(customerRequestDto.nationalId());
        }
    }

    /**
     * Valida um customer existente antes de atualizar.
     */
    public void validateForUpdate(final Customer customerToUpdate, final CustomerRequestDto customerRequestDto) {
        log.info("Validating Customer with ID {} for update with {}", customerToUpdate.getId(), customerRequestDto);
        // valida email duplicado, somente se alterou
        if (!customerToUpdate.getEmail().equals(customerRequestDto.email()) &&
                isEmailAlreadyInUse(customerRequestDto.email()))
        {
            throw new CustomerEmailAlreadyInUseException(customerRequestDto.email());
        }

        // valida nationalId duplicado, somente se alterou
        if (!customerToUpdate.getNationalId().equals(customerRequestDto.nationalId()) &&
                isNationalIdAlreadyInUse(customerRequestDto.nationalId()))
        {
            throw new CustomerNationalIdAlreadyInUseException(customerRequestDto.nationalId());
        }
    }

    private boolean isEmailAlreadyInUse(final String customerEmail) {
        return repository.existsByEmail(customerEmail);
    }

    private boolean isNationalIdAlreadyInUse(final String customerNationalId) {
        return repository.existsByNationalId(customerNationalId);
    }
}
