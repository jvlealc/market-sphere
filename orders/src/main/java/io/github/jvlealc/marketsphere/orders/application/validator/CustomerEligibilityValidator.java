package io.github.jvlealc.marketsphere.orders.application.validator;

import io.github.jvlealc.marketsphere.orders.application.exception.CustomerInactiveException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public final class CustomerEligibilityValidator {

    public void validateActive(CustomerProfile customer) {
        if (!customer.active()) {
            throw new CustomerInactiveException("customerId", "Customer is inactive.");
        }
        log.info("[CustomerValidator] Active Customer verified with ID '{}'.", customer.customerId());
    }
}
