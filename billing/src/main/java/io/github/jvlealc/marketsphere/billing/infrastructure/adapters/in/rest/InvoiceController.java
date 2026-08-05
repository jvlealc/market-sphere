package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.rest;

import io.github.jvlealc.marketsphere.billing.application.model.document.InvoiceDocumentUrl;
import io.github.jvlealc.marketsphere.billing.application.usecase.GetInvoiceDocumentUrlUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final GetInvoiceDocumentUrlUseCase getInvoiceDocumentUrlUseCase;

    @GetMapping("/{invoiceId}/document")
    public ResponseEntity<Void> getInvoiceDocument(@PathVariable UUID invoiceId) {
        InvoiceDocumentUrl documentUrl = getInvoiceDocumentUrlUseCase.execute(invoiceId);

        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .location(documentUrl.value())
                .build();
    }
}
