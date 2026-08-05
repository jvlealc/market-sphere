package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.storage.minio;

import io.github.jvlealc.marketsphere.billing.application.model.document.GeneratedInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.model.document.InvoiceDocumentUrl;
import io.github.jvlealc.marketsphere.billing.application.model.document.RetrievedInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.model.document.StoredInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceDocumentStoragePort;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceDocumentUrlProviderPort;
import io.github.jvlealc.marketsphere.billing.infrastructure.config.props.MinioProps;
import io.github.jvlealc.marketsphere.billing.infrastructure.exception.InvoiceDocumentStorageException;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
class MinioInvoiceDocumentStorageAdapter
        implements InvoiceDocumentStoragePort, InvoiceDocumentUrlProviderPort {

    private static final String STORAGE_KEY_PATTERN = "invoices/orders/%d/%s.%s";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MinioClient minioClient;
    private final MinioProps minioProps;

    @Override
    public StoredInvoiceDocument store(UUID invoiceId, Long orderId, GeneratedInvoiceDocument document) {
        String storageKey = buildStorageKey(invoiceId, orderId, document.fileExtension());
        byte[] content = document.content();

        try (var inputStream = new ByteArrayInputStream(content)) {
            PutObjectArgs objectArgs = PutObjectArgs.builder()
                    .bucket(minioProps.bucketName())
                    .object(storageKey)
                    .stream(inputStream, content.length, -1)
                    .contentType(document.contentType())
                    .build();

            minioClient.putObject(objectArgs);

            return new StoredInvoiceDocument(storageKey);

        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | ErrorResponseException |
                 InsufficientDataException | InternalException | InvalidResponseException | ServerException |
                 XmlParserException exception) {
            throw new InvoiceDocumentStorageException("""
                    Failed to store invoice document.
                        Invoice ID: %s
                        Order ID: %d
                        Storage Key: %s
                    """
                    .formatted(invoiceId, orderId, storageKey),
                    exception
            );
        }
    }

    /**
     * Devolve {@link RetrievedInvoiceDocument}, e não {@link GeneratedInvoiceDocument}: a
     * {@code fileExtension} é insumo da escrita e já está gravada dentro da própria {@code storageKey}.
     * <p>
     * Uma única ida ao servidor — o {@code Content-Type} vem nos headers da resposta do {@code getObject},
     * dispensando um {@code statObject}. A resposta é {@code Closeable} e roda dentro do lease do worker.
     */
    @Override
    public RetrievedInvoiceDocument retrieve(String storageKey) {
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProps.bucketName())
                        .object(storageKey)
                        .build()
        )) {
            byte[] content = response.readAllBytes();
            String contentType = response.headers().get(CONTENT_TYPE_HEADER);

            return new RetrievedInvoiceDocument(
                    content,
                    contentType == null || contentType.isBlank() ? DEFAULT_CONTENT_TYPE : contentType
            );

        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | ErrorResponseException |
                 InsufficientDataException | InternalException | InvalidResponseException | ServerException |
                 XmlParserException exception) {
            throw new InvoiceDocumentStorageException("""
                    Failed to retrieve invoice document.
                        Storage Key: %s
                    """
                    .formatted(storageKey),
                    exception
            );
        }
    }

    @Override
    public InvoiceDocumentUrl provideFor(String storageKey) {
        Duration ttl = Duration.ofMinutes(minioProps.urlExpirationMinutes());

        Instant expiresAt = Instant.now().plus(ttl);

        try {
            GetPresignedObjectUrlArgs presignedUrlArgs = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProps.bucketName())
                    .object(storageKey)
                    .expiry((int) ttl.toSeconds(), TimeUnit.SECONDS)
                    .build();

            URI url = new URI(minioClient.getPresignedObjectUrl(presignedUrlArgs));

            return new InvoiceDocumentUrl(url, expiresAt);

        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | ErrorResponseException |
                 InsufficientDataException | InternalException | InvalidResponseException | ServerException |
                 XmlParserException | URISyntaxException exception) {
            throw new InvoiceDocumentStorageException("""
                    Failed to provide temporary URL for invoice document.
                        Storage Key: %s
                    """
                    .formatted(storageKey),
                    exception
            );
        }
    }

    private static String buildStorageKey(UUID invoiceId, Long orderId, String fileExtension) {
        return STORAGE_KEY_PATTERN.formatted(orderId, invoiceId, fileExtension);
    }
}
