package io.github.jvlealc.marketsphere.orders.service.webhook;

import io.github.jvlealc.marketsphere.orders.dto.webhook.PaymentNotificationDto;

public interface PaymentWebhookService {

    void updatePaymentStatus(PaymentNotificationDto paymentNotificationDto);
}
