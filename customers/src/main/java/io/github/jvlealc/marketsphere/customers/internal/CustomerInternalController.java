package io.github.jvlealc.marketsphere.customers.internal;

import io.github.jvlealc.marketsphere.customers.dto.CustomerResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Expõe endpoints internos destinados às operações internas da corporação.
 * Esses endpoints não fazem parte da API pública de clientes.
 */
@RestController
@RequestMapping("customers/internal")
@RequiredArgsConstructor
@Validated
class CustomerInternalController {

    private final CustomerInternalOperation customerOps;

    /**
     * Busca um cliente pelo seu ID, esteja ele <strong>ativo</strong> ou <strong>inativo</strong>
     * @param customerId ID do cliente
     */
    @GetMapping(value = "{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CustomerResponse> getCustomerByIdIncludingInactive(
            @PathVariable @Positive(message = "{customer.id.positive}") long customerId
    ) {
        return ResponseEntity.ok(customerOps.getCustomerByIdIncludingInactive(customerId));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(50L) int size
    ) {
        return ResponseEntity.ok(
                new PagedModel<>(customerOps.getAllCustomers(page, size))
        );
    }
}
