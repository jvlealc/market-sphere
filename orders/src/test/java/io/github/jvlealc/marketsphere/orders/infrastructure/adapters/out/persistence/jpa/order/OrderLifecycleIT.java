package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.persistence.jpa.order;

import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import io.github.jvlealc.marketsphere.orders.domain.model.OrderItem;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.CancellationInitiator;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.PaymentType;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.CancellationInfo;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.CustomerSnapshot;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.PaymentInfo;
import io.github.jvlealc.marketsphere.orders.support.PostgresContainerSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.BILLED;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.CANCELED;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.PAID;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.PAYMENT_ERROR;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.PAYMENT_PENDING;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.PREPARING_SHIPMENT;
import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os onze CHECK de coerência da tabela {@code orders} só são exercidos por dado que chegue ao banco, o
 * {@code ddl-auto=validate} confere colunas e tipos. Este teste percorre o ciclo de vida
 * real do agregado gravando a cada transição, e prova que as regras bidirecionais aceitam o caminho que a
 * aplicação percorre.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaOrderRepositoryAdapter.class,
        OrderJpaEntityMapper.class,
        OrderItemJpaEntityMapperImpl.class,
        PaymentInfoJpaEntityMapperImpl.class,
        CancellationInfoJpaEntityMapperImpl.class
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OrderLifecycleIT extends PostgresContainerSupport {

    private static final long CUSTOMER_ID = 1L;

    private static final Instant PAID_AT      = Instant.parse("2026-09-01T10:00:00Z");
    private static final Instant BILLED_AT    = Instant.parse("2026-09-01T11:00:00Z");
    private static final Instant SHIPPED_AT   = Instant.parse("2026-09-01T12:00:00Z");

    private static final String PAYMENT_KEY   = "pk-1";
    private static final String RETRY_KEY     = "pk-2";
    private static final String INVOICE_ID    = "019ff81e-2e41-7c42-b1fa-2a239481cb3e";
    private static final String TRACKING_CODE = "BR-2ijs7Su29DaA5";

    private static final BigDecimal EXPECTED_TOTAL = new BigDecimal("650.00");

    private final JpaOrderRepositoryAdapter repository;
    private final EntityManager entityManager;

    OrderLifecycleIT(JpaOrderRepositoryAdapter repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    /** O ciclo completo sem falhas, uma gravação por transição. */
    @Test
    void shouldPersistEveryTransitionOfTheLifecycle() {
        Order pending = store(newOrder());
        assertThat(pending.getId()).isNotNull();
        assertThat(pending.getStatus()).isEqualTo(PAYMENT_PENDING);
        assertThat(pending.getTotal()).isEqualByComparingTo(EXPECTED_TOTAL);

        Order awaiting = reload(pending.getId());
        awaiting.registerPaymentRequest(PAYMENT_KEY);
        store(awaiting);
        assertThat(reload(pending.getId()).getPaymentKey()).isEqualTo(PAYMENT_KEY);

        Order paid = reload(pending.getId());
        paid.markAsPaid(PAYMENT_KEY, PAID_AT);
        store(paid);
        assertStoredStatus(pending.getId(), PAID);

        Order billed = reload(pending.getId());
        billed.markAsBilled(INVOICE_ID, BILLED_AT);
        store(billed);
        assertStoredStatus(pending.getId(), BILLED);

        Order preparing = reload(pending.getId());
        preparing.markAsPreparingShipment();
        store(preparing);
        assertStoredStatus(pending.getId(), PREPARING_SHIPMENT);

        Order shipped = reload(pending.getId());
        shipped.markAsShipped(TRACKING_CODE, SHIPPED_AT);
        store(shipped);

        Order stored = reload(pending.getId());
        assertThat(stored.getStatus()).isEqualTo(SHIPPED);
        assertThat(stored.getPaidAt()).isEqualTo(PAID_AT);
        assertThat(stored.getBilledAt()).isEqualTo(BILLED_AT);
        assertThat(stored.getShippedAt()).isEqualTo(SHIPPED_AT);
        assertThat(stored.getInvoiceId()).isEqualTo(INVOICE_ID);
        assertThat(stored.getTrackingCode()).isEqualTo(TRACKING_CODE);
    }

    /**
     * Os dados congelados na compra voltam do banco iguais. O snapshot do cliente são doze colunas da
     * tabela {@code orders}: colunas trocadas no mapeamento só aparecem comparando o objeto inteiro.
     */
    @Test
    void shouldRestoreFrozenSnapshotsFromTheDatabase() {
        Long orderId = store(newOrder()).getId();

        Order stored = reload(orderId);

        assertThat(stored.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(stored.getCustomerSnapshot()).isEqualTo(customerSnapshot());
        assertThat(stored.getPaymentInfo().getPaymentType()).isEqualTo(PaymentType.DEBIT);
        assertThat(stored.getOrderItems())
                .extracting(OrderItem::getProductName)
                .containsExactlyInAnyOrder("Product 1", "Product 2");
    }

    /**
     * A retentativa de pagamento devolve o pedido a {@code PAYMENT_PENDING} carregando a chave nova, e o
     * {@code chk_orders_paid_at_matches_status} exige que nenhum instante de pagamento tenha sido gravado.
     */
    @Test
    void shouldPersistPaymentFailureAndRecovery() {
        Long orderId = store(newOrder()).getId();

        Order awaiting = reload(orderId);
        awaiting.registerPaymentRequest(PAYMENT_KEY);
        store(awaiting);

        Order failing = reload(orderId);
        failing.markPaymentAsFailed(PAYMENT_KEY, "Card declined");
        store(failing);

        Order failed = reload(orderId);
        assertThat(failed.getStatus()).isEqualTo(PAYMENT_ERROR);
        assertThat(failed.getPaidAt()).isNull();

        failed.registerPaymentRequest(RETRY_KEY);
        store(failed);
        assertStoredStatus(orderId, PAYMENT_PENDING);

        Order retried = reload(orderId);
        retried.markAsPaid(RETRY_KEY, PAID_AT);
        store(retried);

        Order paid = reload(orderId);
        assertThat(paid.getStatus()).isEqualTo(PAID);
        assertThat(paid.getPaymentKey()).isEqualTo(RETRY_KEY);
    }

    /** Cancelamento antes de qualquer pagamento: a linha não pode carregar marco nenhum. */
    @Test
    void shouldPersistCancellationOfPendingOrder() {
        Long orderId = store(newOrder()).getId();

        Order order = reload(orderId);
        order.cancel(cancellationInfo());
        store(order);

        Order stored = reload(orderId);
        assertThat(stored.getStatus()).isEqualTo(CANCELED);
        assertThat(stored.getPaidAt()).isNull();
        assertThat(stored.getBilledAt()).isNull();
        assertThat(stored.getCancellationInfo()).isNotNull();
    }

    /**
     * Cancelamento depois do faturamento. É o caso que exercita os quatro {@code chk_orders_canceled_*} de
     * uma vez: sem dados de envio, faturamento coerente com a nota, faturado implicando pago e pago
     * implicando chave.
     */
    @Test
    void shouldPreserveEarlierMilestonesWhenBilledOrderIsCanceled() {
        Long orderId = store(newOrder()).getId();

        Order order = reload(orderId);
        order.registerPaymentRequest(PAYMENT_KEY);
        order.markAsPaid(PAYMENT_KEY, PAID_AT);
        order.markAsBilled(INVOICE_ID, BILLED_AT);
        store(order);

        Order billed = reload(orderId);
        billed.cancel(cancellationInfo());
        store(billed);

        Order stored = reload(orderId);
        assertThat(stored.getStatus()).isEqualTo(CANCELED);
        assertThat(stored.getPaidAt()).isEqualTo(PAID_AT);
        assertThat(stored.getBilledAt()).isEqualTo(BILLED_AT);
        assertThat(stored.getInvoiceId()).isEqualTo(INVOICE_ID);
        assertThat(stored.getShippedAt()).isNull();
        assertThat(stored.getTrackingCode()).isNull();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * O {@code flush} emite o SQL no passo corrente, em vez de deixá-lo para o autoflush de alguma
     * leitura posterior; o {@code clear} força a leitura seguinte a vir do banco.
     */
    private Order store(Order order) {
        Order saved = repository.save(order);
        entityManager.flush();
        entityManager.clear();

        return saved;
    }

    private Order reload(Long orderId) {
        return repository.findWithDetailsById(orderId).orElseThrow();
    }

    private void assertStoredStatus(Long orderId, OrderStatus expected) {
        assertThat(reload(orderId).getStatus()).isEqualTo(expected);
    }

    private static Order newOrder() {
        return Order.createNew(CUSTOMER_ID, customerSnapshot(), paymentInfo(), items());
    }

    private static CustomerSnapshot customerSnapshot() {
        return new CustomerSnapshot(
                "John Doe", "11122233344", "john.doe@example.com", "5571999998888",
                "41111222", "Rua Primavera", "1A", "", "Alves de Souza",
                "Paulo Afonso", "BA", "Brazil"
        );
    }

    private static PaymentInfo paymentInfo() {
        return PaymentInfo.createNew("4115", PaymentType.DEBIT);
    }

    private static List<OrderItem> items() {
        return List.of(
                OrderItem.createNew(1L, "Product 1", 1, new BigDecimal("150.00")),
                OrderItem.createNew(2L, "Product 2", 5, new BigDecimal("100.00"))
        );
    }

    private static CancellationInfo cancellationInfo() {
        return CancellationInfo.createNew(CancellationInitiator.CUSTOMER, "Changed my mind");
    }
}
