package io.github.jvlealc.marketsphere.orders.application.policy;

import io.github.jvlealc.marketsphere.orders.application.exception.CustomerInactiveException;
import io.github.jvlealc.marketsphere.orders.application.model.customer.CustomerProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public final class CustomerEligibilityPolicy {

    public void ensureActive(CustomerProfile customer) {
        if (!customer.active()) {
            throw new CustomerInactiveException("customerId", "Customer is inactive.");
        }

        log.debug("Active Customer verified with ID '{}'.", customer.customerId());
    }
}
