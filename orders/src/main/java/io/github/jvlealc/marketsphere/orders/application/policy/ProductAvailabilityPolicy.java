package io.github.jvlealc.marketsphere.orders.application.policy;

import io.github.jvlealc.marketsphere.orders.application.exception.ProductNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.exception.ProductUnavailableException;
import io.github.jvlealc.marketsphere.orders.application.model.product.ProductSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public final class ProductAvailabilityPolicy {

    public void ensureAvailable(List<Long> requestedProductIds, Map<Long, ProductSnapshot> foundProducts) {
        Objects.requireNonNull(requestedProductIds, "requestedProductIds must not be null");
        Objects.requireNonNull(foundProducts, "foundProducts must not be null");

        List<Long> distinctRequestedIds = requestedProductIds.stream()
                .distinct()
                .toList();

        ensureAllProductsAreFound(distinctRequestedIds, foundProducts);
        ensureAllProductsAreActive(distinctRequestedIds, foundProducts);

        log.debug("All Products verified and active: '{}'.", distinctRequestedIds);
    }

    private static void ensureAllProductsAreFound(List<Long> requestedProductIds, Map<Long, ProductSnapshot> foundProducts) {
        List<Long> missingIds = requestedProductIds.stream()
                .filter(id -> !foundProducts.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ProductNotFoundException("productId", "Products not found in catalog: " + missingIds);
        }
    }

    private static void ensureAllProductsAreActive(List<Long> requestedProductIds, Map<Long, ProductSnapshot> foundProducts) {
        List<Long> inactiveIds = requestedProductIds.stream()
                .map(foundProducts::get)
                .filter(product -> !product.active())
                .map(ProductSnapshot::id)
                .toList();

        if (!inactiveIds.isEmpty()) {
            throw new ProductUnavailableException("productIds", "Products are inactive: " + inactiveIds);
        }
    }
}