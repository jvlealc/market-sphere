package io.github.jvlealc.marketsphere.orders.payment;

import io.github.jvlealc.marketsphere.orders.order.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.order.model.Order;
import io.github.jvlealc.marketsphere.orders.order.repository.OrderRepository;
import io.github.jvlealc.marketsphere.orders.order.service.OrderLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {

    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService; // Dependência de delegação

    @Transactional
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
