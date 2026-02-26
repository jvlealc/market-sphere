package io.github.jvlealc.marketsphere.orders.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomersClientConfig {

    @Value("${market-sphere.internal-services.customers.config.security.api-key}")
    private String customersServiceApiKey;

    @Bean
    public RequestInterceptor customersServiceApiKeyInterceptor() {
        return template -> {
            template.header("X-Internal-Service-Auth", customersServiceApiKey);
        };
    }
}
