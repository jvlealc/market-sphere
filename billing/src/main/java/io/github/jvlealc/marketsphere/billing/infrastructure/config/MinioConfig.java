package io.github.jvlealc.marketsphere.billing.infrastructure.config;

import io.github.jvlealc.marketsphere.billing.infrastructure.config.props.MinioProps;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProps props) {
        return MinioClient.builder()
                .endpoint(props.url())
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }
}
