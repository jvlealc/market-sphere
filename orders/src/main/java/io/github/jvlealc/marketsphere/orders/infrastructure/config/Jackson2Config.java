package io.github.jvlealc.marketsphere.orders.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Jackson2Config {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // Adicionando plugins e configurações de serialização
            builder.modules(new Jdk8Module(), new JavaTimeModule()); // suporte para tipos do JDK 8 // suporte para serializar/desserializar os tipos do Java Time API
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Garante a serialização de datas como String (ISO 8601) em vez de timestamps numéricos (Epoch time).
            // builder.serializationInclusion(JsonInclude.Include.NON_NULL); // Configura para não incluir campos com valor nulo no JSON gerado.
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // Tolerar campos desconhecidos na desserialização
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES); // Evita falhas ao desserializar propriedades que aparecem em uma JsonView inesperada.
        };
    }
}
