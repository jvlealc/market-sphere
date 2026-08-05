package io.github.jvlealc.marketsphere.billing.infrastructure.config.props;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "minio")
@Validated
public record MinioProps(
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String bucketName,
        @NotBlank String url,
        @Positive int urlExpirationMinutes
) {

    @Override
    public @NonNull String toString() {
        return "MinioProps[url=%s, bucketName=%s, accessKey=%s, secretKey=***, urlExpirationMinutes=%d]"
                .formatted(url, bucketName, accessKey, urlExpirationMinutes);
    }
}