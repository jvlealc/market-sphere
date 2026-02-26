package io.github.jvlealc.marketsphere.orders.service.webhook;

import io.github.jvlealc.marketsphere.orders.dto.webhook.PaymentNotificationDto;
import io.github.jvlealc.marketsphere.orders.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.model.Order;
import io.github.jvlealc.marketsphere.orders.repository.OrderRepository;
import io.github.jvlealc.marketsphere.orders.service.OrderLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService; // Dependência de delegação

    @Transactional
    @Override
    public void updatePaymentStatus(PaymentNotificationDto paymentNotificationDto) {
        Long orderId = paymentNotificationDto.orderId();
        String orderPaymentKey = paymentNotificationDto.paymentKey();
        Order existingOrder = orderRepository.findByIdAndPaymentKey(orderId, orderPaymentKey)
                .orElseThrow(() -> {
                    String errorMessage = String.format("Not found order with ID '%d' and payment key '%s'", orderId, orderPaymentKey);
                    log.error(errorMessage);
                    return new OrderNotFoundException(errorMessage);
                });

        if (paymentNotificationDto.successful()) {
            orderLifecycleService.processSuccessfulPayment(existingOrder.getId());
        } else {
            orderLifecycleService.processPaymentError(existingOrder, paymentNotificationDto.observations());
        }
    }
}
