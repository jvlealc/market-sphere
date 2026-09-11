package io.github.jvlealc.marketsphere.orders.application.customer;

public interface CustomerGatewayPort {

    CustomerProfile getCustomerById(Long customerId);
}
