package io.github.jvlealc.marketsphere.customers.mapper;

import io.github.jvlealc.marketsphere.customers.dto.AddressRequest;
import io.github.jvlealc.marketsphere.customers.dto.AddressResponse;
import io.github.jvlealc.marketsphere.customers.model.Address;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AddressRestMapper {

    private static final String BRAZIL_COUNTRY_CODE = "BR";

    public Address toEntity(AddressRequest request) {
        Objects.requireNonNull(request, "request must not be null.");

        Address address = new Address();
        address.setPostalCode(request.postalCode());
        address.setStreet(request.street());
        address.setHouseNumber(request.houseNumber());
        address.setComplement(request.complement());
        address.setNeighborhood(request.neighborhood());
        address.setCity(request.city());
        address.setState(request.state());
        address.setCountry(BRAZIL_COUNTRY_CODE);

        return address;
    }

    public AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getPostalCode(),
                address.getStreet(),
                address.getHouseNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState(),
                address.getCountry()
        );
    }
}
