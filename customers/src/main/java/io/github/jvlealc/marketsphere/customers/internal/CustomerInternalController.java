package io.github.jvlealc.marketsphere.customers.internal;

import io.github.jvlealc.marketsphere.customers.dto.CustomerResponse;
import io.github.jvlealc.marketsphere.customers.shared.rest.pagination.PageModel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Expõe endpoints internos destinados às operações internas da corporação.
 * <p>
 * Esses endpoints não fazem parte da API pública de clientes.
 */
@RestController
@RequestMapping("internal/customers")
@RequiredArgsConstructor
@Validated
class CustomerInternalController {

    private final CustomerInternalService customerInternalService;

    /**
     * Busca um cliente pelo seu ID, esteja ele <strong>ativo</strong> ou <strong>inativo</strong>
     * @param customerId ID do cliente
     */
    @GetMapping(value = "{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CustomerResponse> getCustomerByIdIncludingInactive(
            @PathVariable @Positive(message = "{customer.id.positive}") long customerId
    ) {
        return ResponseEntity.ok(customerInternalService.getCustomerByIdIncludingInactive(customerId));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageModel<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0")
            @PositiveOrZero(message = "{pagination.pageNumber.positiveOrZero}")
            int pageNumber,
            @RequestParam(defaultValue = "20")
            @Positive(message = "{pagination.pageSize.positive}")
            @Max(value = 50L, message = "{pagination.pageSize.max}")
            int pageSize
    ) {
        return ResponseEntity.ok(
                customerInternalService.getAllCustomers(pageNumber, pageSize)
        );
    }
}
