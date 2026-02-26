package io.github.jvlealc.marketsphere.orders.service;

import io.github.jvlealc.marketsphere.orders.dto.OrderDetailsResponseDto;
import io.github.jvlealc.marketsphere.orders.dto.OrderRequestDto;
import io.github.jvlealc.marketsphere.orders.dto.OrderResponseDto;
import io.github.jvlealc.marketsphere.orders.model.enums.PaymentType;

public interface OrderService {

    OrderResponseDto createOrder(OrderRequestDto orderRequestDto);
    void initiatePayment(Long orderId, String cardInfo, PaymentType paymentType);
    OrderResponseDto getOrderById(Long orderId);
    OrderDetailsResponseDto getOrderDetailsById(Long orderId);
}
