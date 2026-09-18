package io.github.jvlealc.marketsphere.customers.mapper;

import io.github.jvlealc.marketsphere.customers.dto.CustomerRequest;
import io.github.jvlealc.marketsphere.customers.dto.CustomerResponse;
import io.github.jvlealc.marketsphere.customers.model.Address;
import io.github.jvlealc.marketsphere.customers.model.Customer;
import io.github.jvlealc.marketsphere.customers.shared.rest.pagination.PageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CustomerRestMapper {

    private final AddressRestMapper addressMapper;

    public Customer toNewEntity(CustomerRequest request) {
        Objects.requireNonNull(request, "request must not be null.");

        Customer customer = new Customer();
        customer.setFullName(request.fullName());
        customer.setNationalId(request.nationalId());
        customer.setEmail(request.email());
        customer.setPhoneNumber(request.phoneNumber());

        return customer;
    }

    public CustomerResponse toResponse(Customer customer) {
        Objects.requireNonNull(customer, "customer must not be null");
        Address address = customer.getAddress();

        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getNationalId(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                address != null ? addressMapper.toResponse(address) : null,
                customer.isActive()
        );
    }

    public PageModel<CustomerResponse> toPageModel(Page<Customer> page) {
        Objects.requireNonNull(page, "page must not be null");

        List<CustomerResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageModel<>(
                responses,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
