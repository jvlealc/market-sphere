package io.github.jvlealc.marketsphere.orders.infrastructure.client.product;

import feign.FeignException;
import io.github.jvlealc.marketsphere.orders.application.exception.ExternalServiceException;
import io.github.jvlealc.marketsphere.orders.application.exception.ProductNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.product.ProductGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.product.ProductSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductFeignGatewayAdapter implements ProductGatewayPort {

    private final ProductFeignClient productFeignClient;

    public ProductSnapshot getProductById(Long productId) {
        try {
            ResponseEntity<ProductRepresentation> response = productFeignClient.getProductById(productId);

            ProductRepresentation representation = Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("Product service returned a null body (200 OK) for productId: {}.", productId);
                        return new ProductNotFoundException("productId", "Product not found or returned an empty response for ID: " + productId);
                    });

            return toSnapshot(representation);

        } catch (FeignException.NotFound e) {
            log.warn("Product not found (404) via Feign client for productId: {}. Message: {}", productId, e.getMessage());
            throw new ProductNotFoundException("productId", "Product not found with ID: " + productId);
        } catch (FeignException e) {
            log.error("Error while calling product service. For productId: {}. Status: {}. Message: {}", productId, e.status(), e.getMessage());
            throw new ExternalServiceException("Error while calling product service. For product ID: " + productId, e);
        }
    }

    public Map<Long, ProductSnapshot> getProductsByIdsIncludingInactive(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            ResponseEntity<List<ProductRepresentation>> response = productFeignClient.getAllProductsByIds(productIds);

            List<ProductRepresentation> representations = Optional.ofNullable(response.getBody())
                    .orElse(Collections.emptyList());

            return toSnapshotMap(representations);
        } catch (FeignException.NotFound e) {
            log.warn("[Products Batch Lookup] No products found via Feign for IDs: {}. message={}", productIds, e.getMessage());
            return Collections.emptyMap();
        } catch (FeignException e) {
            log.error("Error while calling product service. For productIds: {}. Status: {}. Message: {}", productIds, e.status(), e.getMessage());
            throw new ExternalServiceException("Error while calling product service. For productIds: " + productIds, e);
        }
    }

    private static ProductSnapshot toSnapshot(ProductRepresentation representation) {
        return new ProductSnapshot(
                representation.id(),
                representation.name(),
                representation.unitPrice(),
                representation.description(),
                representation.active()
        );
    }

    private static Map<Long, ProductSnapshot> toSnapshotMap(List<ProductRepresentation> representations) {
        return representations.stream()
                .map(ProductFeignGatewayAdapter::toSnapshot)
                .collect(Collectors.toMap(
                        ProductSnapshot::id,
                        product -> product
                ));
    }
}
