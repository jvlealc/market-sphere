package io.github.jvlealc.marketsphere.orders.facade;

import io.github.jvlealc.marketsphere.orders.client.banking.BankingClient;
import io.github.jvlealc.marketsphere.orders.client.banking.representation.BankingPaymentRepresentation;
import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.facade.client.CustomerClientService;
import io.github.jvlealc.marketsphere.orders.facade.client.ProductClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Implementação da Facade principal.
 */
@Component
@RequiredArgsConstructor
public class OrderDependenciesFacadeImpl implements OrderDependenciesFacade {

    // Injeta as abstrações dos Serviços de Cliente
    private final CustomerClientService customerClientService;
    private final ProductClientService productClientService;
    private final BankingClient bankingClient;

    @Override
    public CustomerRepresentation getCustomerById(Long customerId) {
        return customerClientService.getCustomerById(customerId);
    }

    @Override
    public ProductRepresentation getProductById(Long productId) {
        return productClientService.getProductById(productId);
    }

    @Override
    public Map<Long, ProductRepresentation> getProductsByIds(List<Long> productIds) {
        return productClientService.getProductsByIds(productIds);
    }

    @Override
    public BankingPaymentRepresentation requestPayment(Long orderId) {
        return bankingClient.requestPayment(orderId);
    }

    @Override
    public CustomerRepresentation getCustomerByIdIgnoringFilter(Long customerId) {
        return customerClientService.getCustomerByIdIgnoringFilter(customerId);
    }
}
