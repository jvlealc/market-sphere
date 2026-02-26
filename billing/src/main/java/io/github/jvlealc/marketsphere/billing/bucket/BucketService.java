package io.github.jvlealc.marketsphere.billing.bucket;

import io.github.jvlealc.marketsphere.billing.bucket.exception.StorageAccessException;
import io.github.jvlealc.marketsphere.billing.config.props.MinioProps;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BucketService {

    private final MinioClient minioClient;
    private final MinioProps minioProps;

    public BucketService(MinioClient minioClient, MinioProps minioProps) {
        this.minioClient = minioClient;
        this.minioProps = minioProps;
    }

    /**
     * Realizar upload de arquivos para o Cloud bucket
     * */
    public void upload(BucketFile file) {
        try {
            PutObjectArgs objectArgs = PutObjectArgs.builder()
                    .bucket(minioProps.getBucketName())
                    .object(file.name())
                    .stream(file.inputStream(), file.size(), -1)
                    .contentType(file.mediaType().toString())
                    .build();
            minioClient.putObject(objectArgs); // Adicionar objetos ao bucket
        } catch (Exception e) {
            log.error("MINIO UPLOAD FAILED! Root cause: ", e);
            throw new StorageAccessException("Failed to upload file: " + file.name(), e);
        }
    }

    /**
     *  Retorna a url para se obter o arquivo LOCALMENTE
     * */
    public String generatePresignedUrl(String fileName) {
        final int expiryTimeInDays = 5;
        try {
            GetPresignedObjectUrlArgs presignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProps.getBucketName())
                    .object(fileName)
                    .expiry(expiryTimeInDays, TimeUnit.DAYS)
                    .build();
           return minioClient.getPresignedObjectUrl(presignedObjectUrlArgs);
        } catch (Exception e) {
            throw new StorageAccessException("Failed to generate presigned URL for file: " + fileName, e);
        }
    }

    /**
     * Baixa um arquivo do bucket.
     * Este méto-do busca os metadados e o stream do arquivo.
     *
     * @param fileName O nome do objeto no bucket.
     * @return Um {@link BucketFile} contendo o stream e os metadados.
     */
    public final BucketFile download(final String fileName) {
        try {
            // Buscar metadados
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProps.getBucketName())
                            .object(fileName)
                            .build()
            );

            long size = stat.size();
            MediaType mediaType = MediaType.parseMediaType(stat.contentType());

            // Buscar o arquivo
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProps.getBucketName())
                            .object(fileName)
                            .build()
            );

            // Retorna o objeto combinado do cloud bucket
            return new BucketFile(fileName, inputStream, mediaType, size);

        } catch (Exception e) {
            log.error("Failed to download file {}. Error: {}. Reason: {}.", fileName, e.getMessage(), e.getCause(), e);
            throw new StorageAccessException("Failed to download file: " + fileName, e);
        }
    }
}
