package io.github.jvlealc.marketsphere.orders.interfaces.rest.webhook;

import io.github.jvlealc.marketsphere.orders.application.usecase.HandlePaymentConfirmationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final HandlePaymentConfirmationUseCase handlePaymentConfirmationUseCase;
    private final PaymentWebhookAuthenticator authenticator;
    private final PaymentWebhookMapper webhookMapper;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handlePaymentConfirmation(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody @Valid PaymentWebhookRequest request
    ) {
        authenticator.authenticate(secret);

        handlePaymentConfirmationUseCase.execute(
                webhookMapper.toPaymentCommand(request)
        );

        return ResponseEntity.noContent().build();
    }
}
