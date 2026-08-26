package io.github.jvlealc.marketsphere.orders.application.model.outbox.payload;

import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.optionalText;
import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.requiredId;
import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.requiredText;

/**
 * {@code complement} e {@code neighborhood} são opcionais porque as colunas correspondentes são nullable
 * no serviço {@code customers}.
 */
public record OrderPaidCustomerPayload(
        Long customerId,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        String postalCode,
        String street,
        String houseNumber,
        String complement,
        String neighborhood,
        String city,
        String state,
        String country
) {

    public OrderPaidCustomerPayload {
        customerId = requiredId(customerId, "Customer ID");
        fullName = requiredText(fullName, "Customer full name");
        nationalId = requiredText(nationalId, "Customer national ID");
        email = requiredText(email, "Customer email");
        phoneNumber = requiredText(phoneNumber, "Customer phone number");
        postalCode = requiredText(postalCode, "Customer postal code");
        street = requiredText(street, "Customer street");
        houseNumber = requiredText(houseNumber, "Customer house number");
        complement = optionalText(complement);
        neighborhood = optionalText(neighborhood);
        city = requiredText(city, "Customer city");
        state = requiredText(state, "Customer state");
        country = requiredText(country, "Customer country");
    }
}
