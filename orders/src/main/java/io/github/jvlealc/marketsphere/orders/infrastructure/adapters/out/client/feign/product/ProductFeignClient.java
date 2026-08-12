package io.github.jvlealc.marketsphere.orders.infrastructure.client.product;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "products",
        url = "${market-sphere.feign.clients.products.base-url}"
)
public interface ProductFeignClient {

    @GetMapping("/{productId}")
    ResponseEntity<ProductRepresentation> getProductById(@PathVariable Long productId);

    @GetMapping()
    ResponseEntity<List<ProductRepresentation>> getAllProductsByIds(@RequestParam("productsIds") List<Long> productsIds);
}
