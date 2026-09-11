package io.github.jvlealc.marketsphere.orders.application.outbox.payload;

import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.optionalText;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredId;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredText;

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
        customerId = requiredId(customerId, "customerId");
        fullName = requiredText(fullName, "fullName");
        nationalId = requiredText(nationalId, "nationalId");
        email = requiredText(email, "email");
        phoneNumber = requiredText(phoneNumber, "phoneNumber");
        postalCode = requiredText(postalCode, "postalCode");
        street = requiredText(street, "street");
        houseNumber = requiredText(houseNumber, "houseNumber");
        complement = optionalText(complement);
        neighborhood = optionalText(neighborhood);
        city = requiredText(city, "city");
        state = requiredText(state, "state");
        country = requiredText(country, "country");
    }
}
