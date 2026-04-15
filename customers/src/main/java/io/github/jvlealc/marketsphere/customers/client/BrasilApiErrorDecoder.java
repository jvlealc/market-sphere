package io.github.jvlealc.marketsphere.customers.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.util.Map;

public class BrasilApiErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            if (response.body() != null) {
                var body = objectMapper.readValue(response.body().asInputStream(), Map.class);
                String name = (String) body.get("name");
                if ("CepPromiseError".equals(name)) {
                    return new BrasilApiException("Postal code service returned business error.");
                }
            }

            // fallback pelo HTTP status
            return switch (response.status()) {
                case 404 -> new BrasilApiException("Postal code not found.");
                case 400 -> new BrasilApiException("Invalid postal code format.");
                default -> new BrasilApiException("Unexpected error calling Brasil API.");
            };

        } catch (IOException ignored) {
            return new BrasilApiException("Error reading Brasil API response");
        }
    }
}
