package io.github.jvlealc.marketsphere.orders.application.validator;

import io.github.jvlealc.marketsphere.orders.application.exception.ProductNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.product.ProductSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public final class ProductAvailabilityValidator {

    public void validateAvailable(List<Long> requestedProductIds, Map<Long, ProductSnapshot> foundProducts) {
        Objects.requireNonNull(requestedProductIds, "Requested product IDs must not be null");
        Objects.requireNonNull(foundProducts, "Found products map must not be null");

        List<Long> distinctRequestedIds = requestedProductIds.stream()
                .distinct()
                .toList();

        validateAllProductsAreFound(distinctRequestedIds, foundProducts);
        validateAllProductsAreActive(distinctRequestedIds, foundProducts);

        log.info("[ProductValidator] All Products verified and active: '{}'.", distinctRequestedIds);
    }

    private static void validateAllProductsAreActive(List<Long> requestedProductIds, Map<Long, ProductSnapshot> foundProducts) {
        List<Long> inactiveIds = requestedProductIds.stream()
                .map(foundProducts::get)
                .filter(product -> !product.active())
                .map(ProductSnapshot::id)
                .toList();

        if (!inactiveIds.isEmpty()) {
            throw new ProductNotFoundException("productIds", "Products are inactive: " + inactiveIds);
        }
    }

    private static void validateAllProductsAreFound(List<Long> requestedProductIds, Map<Long, ProductSnapshot> foundProducts) {
        List<Long> missingIds = requestedProductIds.stream()
                .filter(id -> !foundProducts.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ProductNotFoundException("productId", "Products not found in catalog: " + missingIds);
        }
    }
}