package io.github.jvlealc.marketsphere.orders.validator;

import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.exception.client.customers.CustomerClientNotFoundException;
import io.github.jvlealc.marketsphere.orders.exception.client.products.ProductClientNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderValidator {

    public void validate(List<Long> requestedProductsIds, Map<Long, ProductRepresentation> productsMap, CustomerRepresentation customerRepresentation) {
        validateCustomer(customerRepresentation);
        validateProduct(requestedProductsIds, productsMap);
    }

    private void validateCustomer(CustomerRepresentation customerRepresentation) {
        if (!customerRepresentation.active()) {
            throw new CustomerClientNotFoundException("customerId", "Customer is inactive.");
        }
        log.info("[OrderValidator] Active Customer found with ID '{}'.", customerRepresentation.id());
    }

    private void validateProduct(List<Long> requestedProductsIds, Map<Long, ProductRepresentation> foundProducts) {
        List<Long> missingIds = requestedProductsIds.stream()
                .filter(productId -> !foundProducts.containsKey(productId))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ProductClientNotFoundException("productId", "Products not found in catalog: " + missingIds);
        }

        List<Long> inactiveIds = requestedProductsIds.stream()
                .map(foundProducts::get)
                .filter(product -> !product.active())
                .map(ProductRepresentation::id)
                .toList();

        if (!inactiveIds.isEmpty()) {
            throw new ProductClientNotFoundException("productId", "Products are inactive: " + inactiveIds);
        }

        log.info("[OrderValidator] All Products are found and active with ID's: '{}'.", requestedProductsIds);
    }
}
