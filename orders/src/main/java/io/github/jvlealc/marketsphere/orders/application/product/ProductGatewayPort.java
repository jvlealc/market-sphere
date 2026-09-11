package io.github.jvlealc.marketsphere.orders.application.product;

import java.util.List;
import java.util.Map;

public interface ProductGatewayPort {

    Map<Long, ProductSnapshot> getProductsByIdsIncludingInactive(List<Long> productIds);
}
