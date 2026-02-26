package io.github.jvlealc.marketsphere.customers.client.brasilapi.config;

import io.github.jvlealc.marketsphere.customers.client.brasilapi.error.BrasilApiErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrasilApiFeignConfig {

    @Bean
    public BrasilApiErrorDecoder brasilApiErrorDecoder() {
        return new BrasilApiErrorDecoder();
    }
}
