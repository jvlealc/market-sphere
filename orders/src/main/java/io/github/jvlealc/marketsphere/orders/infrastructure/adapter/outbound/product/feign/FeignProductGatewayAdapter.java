package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.product.feign;

import feign.FeignException;
import io.github.jvlealc.marketsphere.orders.application.ExternalServiceException;
import io.github.jvlealc.marketsphere.orders.application.product.ProductGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.product.ProductSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignProductGatewayAdapter implements ProductGatewayPort {

    // Espelha o teto de productsIds aceito por GET /internal/products/including-inactives no products.
    private static final int MAX_BATCH_SIZE = 50;

    private final ProductFeignClient productFeignClient;

    public Map<Long, ProductSnapshot> getProductsByIdsIncludingInactive(List<Long> productIds) {
        Objects.requireNonNull(productIds, "productIds must not be null");
        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("productIds must not be empty");
        }

        List<Long> distinctIds = productIds.stream()
                .distinct()
                .toList();

        List<ProductRepresentation> representations = partitionProductIds(distinctIds).stream()
                .map(this::fetchBatch)
                .flatMap(List::stream)
                .toList();

        return toSnapshots(representations);
    }

    private List<ProductRepresentation> fetchBatch(List<Long> batchIds) {
        try {
            return productFeignClient.getProductsByIdsIncludingInactives(batchIds);
        } catch (FeignException e) {
            log.error("Error while calling product service. For productIds: {}. Status: {}. Message: {}", batchIds, e.status(), e.getMessage());
            throw new ExternalServiceException("Error while calling product service. For productIds: " + batchIds, e);
        }
    }

    private static List<List<Long>> partitionProductIds(List<Long> productIds) {
        List<List<Long>> batches = new ArrayList<>();
        int size = productIds.size();

        for (int i = 0; i < size; i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, size);
            batches.add(productIds.subList(i, end));
        }

        return batches;
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

    private static Map<Long, ProductSnapshot> toSnapshots(List<ProductRepresentation> representations) {
        return representations.stream()
                .map(FeignProductGatewayAdapter::toSnapshot)
                .collect(Collectors.toMap(
                        ProductSnapshot::id,
                        product -> product
                ));
    }
}
