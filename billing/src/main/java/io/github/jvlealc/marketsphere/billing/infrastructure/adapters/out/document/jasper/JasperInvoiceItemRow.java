package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.document.jasper;

import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidItem;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Fachada JavaBean sobre {@link OrderPaidItem}, porque {@code JRBeanCollectionDataSource} resolve campos
 * por convenção {@code getXxx()} e o record expõe {@code xxx()}.
 * <p>
 * <strong>Precisa ser pública.</strong> O JasperReports lê estes getters via {@code PropertyUtils} do
 * commons-beanutils, e o {@code MethodUtils.getAccessibleMethod} devolve {@code null} para métodos públicos
 * declarados em classe não-pública, sem superclasse ou interface pública que os declare. O resultado seria
 * {@code NoSuchMethodException} na geração do PDF — em runtime, porque nenhum teste cobre este caminho.
 */
public final class JasperInvoiceItemRow {

    private final OrderPaidItem item;

    private JasperInvoiceItemRow(OrderPaidItem item) {
        Objects.requireNonNull(item, "item must not be null");
        this.item = item;
    }

    static JasperInvoiceItemRow from(OrderPaidItem item) {
        return new JasperInvoiceItemRow(item);
    }

    public Long getProductId() {
        return item.productId();
    }

    public String getProductName() {
        return item.productName();
    }

    public BigDecimal getUnitPrice() {
        return item.unitPrice();
    }

    public int getAmount() {
        return item.amount();
    }

    public BigDecimal getSubtotal() {
        return item.subtotal();
    }
}
