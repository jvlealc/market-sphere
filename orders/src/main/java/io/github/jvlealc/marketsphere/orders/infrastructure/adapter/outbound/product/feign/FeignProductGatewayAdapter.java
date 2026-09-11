package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.product.feign;

import feign.FeignException;
import io.github.jvlealc.marketsphere.orders.application.ExternalServiceException;
import io.github.jvlealc.marketsphere.orders.application.product.ProductNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.product.ProductGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.product.ProductSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignProductGatewayAdapter implements ProductGatewayPort {

    private final ProductFeignClient productFeignClient;

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
            log.warn("No products found via Feign for IDs: {}. message={}", productIds, e.getMessage());
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
                .map(FeignProductGatewayAdapter::toSnapshot)
                .collect(Collectors.toMap(
                        ProductSnapshot::id,
                        product -> product
                ));
    }
}
