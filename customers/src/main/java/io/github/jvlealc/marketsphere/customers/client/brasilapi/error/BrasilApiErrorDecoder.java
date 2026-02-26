package io.github.jvlealc.marketsphere.customers.client.brasilapi.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import io.github.jvlealc.marketsphere.customers.exception.client.brasilapi.PostalCodeInvalidException;

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
                    String message = (String) body.get("message");
                    return new PostalCodeInvalidException(message);
                }
            }

            // fallback pelo HTTP status
            return switch (response.status()) {
                case 404 -> new PostalCodeInvalidException("Postal code not found.");
                case 400 -> new PostalCodeInvalidException("Invalid postal code format.");
                default -> new PostalCodeInvalidException("Unexpected error calling Brasil API");
            };
        } catch (IOException ex) {
            return new PostalCodeInvalidException("Error reading Brasil API response", ex);
        }
    }
}
