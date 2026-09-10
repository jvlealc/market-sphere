package io.github.jvlealc.marketsphere.orders.domain.order;

/**
 * Payment types supported in the system.
 */
public enum PaymentType {
    /** Payment via debit card */
    DEBIT,
    /** Payment via credit card */
    CREDIT,
    /** Payment via PayPal */
    PAYPAL,
    /** Payment via PIX */
    PIX
}
