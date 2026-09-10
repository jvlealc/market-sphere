package io.github.jvlealc.marketsphere.customers.client.brasilapi;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BrasilApiPostalCodeValidator {

    private final BrasilApiFeignClient brasilApiClient;

    public void validate(String postalCode) {
        try {
            ResponseEntity<BrasilApiAddressRepresentation> response = brasilApiClient.getAddressByPostalCode(postalCode);
            BrasilApiAddressRepresentation body = response.getBody();

            if (body == null) {
                log.warn("BrasilAPI returned HTTP status {}, but contained a null response body", response.getStatusCode());

                throw new BrasilApiException("Invalid response from address external service.");
            }

        } catch (FeignException.NotFound e) {
            throw new PostalCodeNotFoundException("Postal code not found");
        } catch (FeignException e) {
            log.warn("BrasilAPI internal error", e);

            throw new BrasilApiException("The address validation service is temporarily unavailable. Please try again in a few minutes.", e);
        }
    }
}
