package io.github.jvlealc.marketsphere.customers.config;

import io.github.jvlealc.marketsphere.customers.client.brasilapi.BrasilApiFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(
        basePackages = "io.github.jvlealc.marketsphere.customers.client",
        clients = BrasilApiFeignClient.class
)
class FeignClientConfig {
}
