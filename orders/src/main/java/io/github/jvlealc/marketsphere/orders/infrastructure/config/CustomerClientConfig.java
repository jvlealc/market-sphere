package io.github.jvlealc.marketsphere.orders.infrastructure.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerClientConfig {

    @Value("${market-sphere.internal-services.customers.config.security.api-key}")
    private String customerServiceApiKey;

    @Bean
    public RequestInterceptor customerApiKeyInterceptor() {
        return template -> {
            template.header("X-Internal-Service-Auth", customerServiceApiKey);
        };
    }
}
