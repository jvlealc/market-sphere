package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.product.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "products",
        url = "${market-sphere.feign.clients.products.base-url}"
)
public interface ProductFeignClient {

    @GetMapping
    ResponseEntity<List<ProductRepresentation>> getAllProductsByIds(@RequestParam("productsIds") List<Long> productsIds);
}
