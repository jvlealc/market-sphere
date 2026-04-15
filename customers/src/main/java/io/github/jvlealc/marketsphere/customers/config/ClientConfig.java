package io.github.jvlealc.marketsphere.customers.config;

import io.github.jvlealc.marketsphere.customers.client.BrasilApiErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    @Bean
    public BrasilApiErrorDecoder brasilApiErrorDecoder() {
        return new BrasilApiErrorDecoder();
    }
}
