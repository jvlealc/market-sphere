package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.product.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "products",
        url = "${market-sphere.feign.clients.products.base-url}"
)
public interface ProductFeignClient {

    @GetMapping("/including-inactives")
    List<ProductRepresentation> getProductsByIdsIncludingInactives(@RequestParam("productsIds") List<Long> productsIds);
}
