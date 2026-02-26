package io.github.jvlealc.marketsphere.orders.controller.webhook;

import io.github.jvlealc.marketsphere.orders.dto.webhook.PaymentNotificationDto;
import io.github.jvlealc.marketsphere.orders.service.webhook.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders/payment-callbacks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping
    public ResponseEntity<Void> updatePaymentStatus(
            @RequestBody PaymentNotificationDto paymentNotificationDto,
            @RequestHeader("apiKey") String apiKey
    ) {
        paymentWebhookService.updatePaymentStatus(paymentNotificationDto);
        return ResponseEntity.noContent().build();
    }
}
