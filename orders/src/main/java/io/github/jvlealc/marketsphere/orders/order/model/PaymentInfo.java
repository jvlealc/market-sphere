package io.github.jvlealc.marketsphere.orders.order.model;

import io.github.jvlealc.marketsphere.orders.order.model.enums.PaymentType;
import lombok.Data;

@Data
public class PaymentInfo {
    private String metadata;
    private PaymentType paymentType;
}
