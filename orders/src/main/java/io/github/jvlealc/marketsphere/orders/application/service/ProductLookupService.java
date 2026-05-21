package io.github.jvlealc.marketsphere.orders.application.service;

import io.github.jvlealc.marketsphere.orders.application.ports.out.product.ProductGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.product.ProductSnapshot;
import io.github.jvlealc.marketsphere.orders.application.validator.ProductAvailabilityValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductLookupService {

    private final ProductGatewayPort productGateway;
    private final ProductAvailabilityValidator productValidator;

    public Map<Long, ProductSnapshot> getAvailableProductsByIds(List<Long> productIds) {
        Map<Long, ProductSnapshot> products = productGateway.getProductsByIdsIncludingInactive(productIds);
        productValidator.validateAvailable(productIds, products);
        return products;
    }

    public Map<Long, ProductSnapshot> getProductsByIdsIncludingInactive(List<Long> productIds) {
        return productGateway.getProductsByIdsIncludingInactive(productIds);
    }
}
