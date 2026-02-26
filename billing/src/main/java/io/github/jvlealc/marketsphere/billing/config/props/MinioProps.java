package io.github.jvlealc.marketsphere.billing.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
@EnableConfigurationProperties  // Habilita a capabilidade desta classe representar as propriedades configuradas do application.yml
@ConfigurationProperties(prefix = "minio")
public class MinioProps {

    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String url;

    public MinioProps() {
    }

    public MinioProps(String accessKey, String secretKey, String bucketName, String url) {
        this.accessKey = accessKey;
        this.bucketName = bucketName;
        this.secretKey = secretKey;
        this.url = url;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MinioProps that = (MinioProps) o;
        return Objects.equals(accessKey, that.accessKey) &&
                Objects.equals(secretKey, that.secretKey) &&
                Objects.equals(bucketName, that.bucketName) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessKey, secretKey, bucketName, url);
    }
}
