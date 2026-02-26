package io.github.jvlealc.marketsphere.customers.mapper;

import io.github.jvlealc.marketsphere.customers.dto.CustomerRequestDto;
import io.github.jvlealc.marketsphere.customers.dto.CustomerResponseDto;
import io.github.jvlealc.marketsphere.customers.client.brasilapi.representation.BrasilApiAddressRepresentation;
import io.github.jvlealc.marketsphere.customers.model.Customer;
import io.github.jvlealc.marketsphere.customers.model.vo.Address;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CustomerMapper {

    public Customer toCustomerEntity(
            final CustomerRequestDto customerRequestDto,
            final BrasilApiAddressRepresentation brasilApiAddressRepresentation
    ) {
        Objects.requireNonNull(customerRequestDto, "CustomerRequestDto must not be null.");
        Objects.requireNonNull(brasilApiAddressRepresentation, "BrasilApiAddressDto must not be null.");

        Customer customer = new Customer();
        customer.setFullName(customerRequestDto.fullName());
        customer.setNationalId(customerRequestDto.nationalId());
        customer.setEmail(customerRequestDto.email());
        customer.setPhoneNumber(customerRequestDto.phoneNumber());
        customer.setAddressVo(
                this.adaptAddress(
                        brasilApiAddressRepresentation,
                        customerRequestDto.number(),
                        customerRequestDto.complement(),
                        customerRequestDto.country()
                )
        );
        return customer;
    }

    public CustomerResponseDto toCustomerDto(final Customer customer) {
        Objects.requireNonNull(customer, "Customer must not be null");
        Address addressVo = customer.getAddressVo();
        Objects.requireNonNull(addressVo, "Address must not be null");

        return new CustomerResponseDto(
                customer.getId(),
                customer.getFullName(),
                customer.getNationalId(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                addressVo.getPostalCode(),
                addressVo.getStreet(),
                addressVo.getNumber(),
                addressVo.getComplement(),  // Pode ser nulo
                addressVo.getNeighborhood(), // Pode ser nulo
                addressVo.getCity(),
                addressVo.getState(),
                addressVo.getCountry(),
                customer.isActive()
        );
    }

    public void updateCustomerEntity(
            final Customer customerToUpdate,
            final CustomerRequestDto customerRequestDto,
            final BrasilApiAddressRepresentation brasilApiAddressRepresentation
    ) {
        Objects.requireNonNull(customerToUpdate, "Customer must not be null.");
        Objects.requireNonNull(customerRequestDto, "CustomerRequestDto must not be null.");
        Objects.requireNonNull(brasilApiAddressRepresentation, "BrasilApiAddressDto must not be null.");

        customerToUpdate.setFullName(customerRequestDto.fullName());
        customerToUpdate.setNationalId(customerRequestDto.nationalId());
        customerToUpdate.setEmail(customerRequestDto.email());
        customerToUpdate.setPhoneNumber(customerRequestDto.phoneNumber());

        if (!customerToUpdate.getAddressVo().getPostalCode().equals(brasilApiAddressRepresentation.postalCode())) {
            customerToUpdate.setAddressVo(
                    this.adaptAddress(
                            brasilApiAddressRepresentation,
                            customerRequestDto.number(),
                            customerRequestDto.complement(),
                            customerRequestDto.country()
                    )
            );
        }
    }

    private Address adaptAddress(
            final BrasilApiAddressRepresentation brasilApiAddressRepresentation,
            final String number,
            final String complement,
            final String country
    ) {
         return new Address(
                 brasilApiAddressRepresentation.postalCode(),
                 brasilApiAddressRepresentation.street(),
                 number,
                 complement,
                 brasilApiAddressRepresentation.neighborhood(),
                 brasilApiAddressRepresentation.city(),
                 brasilApiAddressRepresentation.state(),
                 country
         );
    }
}
