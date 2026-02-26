package io.github.jvlealc.marketsphere.billing.bucket;

import org.springframework.http.MediaType;

import java.io.InputStream;

public record BucketFile(
        String name,
        InputStream inputStream,
        MediaType mediaType,
        long size
) { }
