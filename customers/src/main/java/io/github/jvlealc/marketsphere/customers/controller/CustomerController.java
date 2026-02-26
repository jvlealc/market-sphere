package io.github.jvlealc.marketsphere.customers.controller;

import io.github.jvlealc.marketsphere.customers.controller.util.HeaderLocationBuilder;
import io.github.jvlealc.marketsphere.customers.dto.CustomerRequestDto;
import io.github.jvlealc.marketsphere.customers.dto.CustomerResponseDto;
import io.github.jvlealc.marketsphere.customers.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerService service;

    @Value("${market-sphere.internal-services.orders.config.security.api-key}")
    private String expectedOrdersServiceApiKey;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE )
    public ResponseEntity<Void> createCustomer(@RequestBody @Valid CustomerRequestDto customerRequestDto) {
        CustomerResponseDto customerResponseDto = service.createCustomer(customerRequestDto);
        return ResponseEntity
                .created(HeaderLocationBuilder.build(customerResponseDto.id()))
                .build();
    }

    @GetMapping(value = "/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomerResponseDto> getCustomerById(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId
    ) {
        return ResponseEntity.ok(service.getCustomerById(customerId));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        List<CustomerResponseDto> costumers = service.getAllCustomers();
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(costumers.size()))
                .body(costumers);
    }

    /**
     * Realiza a exclusão lógica de um produto
     * @param customerId – ID do cliente a ser inativado
     * @return {@code HTTP Status 204 - No Content} se bem-sucedido
     * */
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomerById(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId
    ) {
        service.deleteCustomerById(customerId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{customerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateCustomer(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId,
            @RequestBody @Valid CustomerRequestDto customerRequestDto
    ) {
        service.updateCustomer(customerId, customerRequestDto);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa um cliente lógicamente excluído
     * @param customerId ID do cliente a ser reativado
     * @return {@code HTTP Status 204 - No Content} se bem-sucedido
     * */
    @PostMapping("/{customerId}/reactivate")
    public ResponseEntity<Void> reactivateCustomerById(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId
    ) {
        service.reactivateCustomerById(customerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint interno (Service-to-Service) para busca um cliente pelo seu ID, esteja ele ativo ou inativo.
     * É destinado **exclusivamente** à comunicação com o microsserviço de Pedidos (Orders).
     *
     * @param customerId ID do cliente
     * @param receivedOrdersServiceApiKey Chave de autenticação secreta enviada no header {@code X-Internal-Service-Auth}.
     *
     * @return {@code ResponseEntity<CustomerResponseDto>} dados do cliente
     */
    @GetMapping(value = "/for-orders-service/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomerResponseDto> getCustomerByIdIgnoringFilter(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId,
            @RequestHeader("X-Internal-Service-Auth") String receivedOrdersServiceApiKey
    ) {
        if (!receivedOrdersServiceApiKey.equals(this.expectedOrdersServiceApiKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok( service.getCustomerByIdIgnoringFilter(customerId) );
    }
}
