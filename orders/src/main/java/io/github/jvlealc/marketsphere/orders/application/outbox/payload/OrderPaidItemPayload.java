package io.github.jvlealc.marketsphere.orders.application.outbox.payload;

import java.math.BigDecimal;

import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredAmount;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredId;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredQuantity;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredText;

public record OrderPaidItemPayload(
        Long productId,
        String productName,
        Integer amount,
        BigDecimal unitPrice
) {

    public OrderPaidItemPayload {
        productId = requiredId(productId, "Product ID");
        productName = requiredText(productName, "Product name");
        amount = requiredQuantity(amount, "Amount");
        unitPrice = requiredAmount(unitPrice, "Unit price");
    }
}
