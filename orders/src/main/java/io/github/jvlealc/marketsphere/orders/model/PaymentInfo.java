package io.github.jvlealc.marketsphere.orders.model;

import io.github.jvlealc.marketsphere.orders.model.enums.PaymentType;
import lombok.Data;

@Data
public class PaymentInfo {
    private String metadata;
    private PaymentType paymentType;
}
