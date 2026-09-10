package io.github.jvlealc.marketsphere.orders.application.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductLookupService {

    private final ProductGatewayPort productGateway;
    private final ProductAvailabilityPolicy productPolicy;

    public Map<Long, ProductSnapshot> getAvailableProductsByIds(List<Long> productIds) {
        Map<Long, ProductSnapshot> products = productGateway.getProductsByIdsIncludingInactive(productIds);
        productPolicy.ensureAvailable(productIds, products);
        return products;
    }
}
