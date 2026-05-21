package io.github.jvlealc.marketsphere.shipping.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Jackson2Config {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectMapper() {
        return builder -> {
            builder.modules(new JavaTimeModule(), new Jdk8Module());
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }
}
