package io.github.jvlealc.marketsphere.orders.infrastructure.persistence;

import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.OrderJpaEntity;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.projection.OrderSummaryJpaProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, Long> {

    @EntityGraph(attributePaths = {"paymentInfo", "orderItems", "cancellationInfo"})
    @Query("""
            SELECT DISTINCT o
            FROM OrderJpaEntity o
            WHERE o.id = :id
            """)
    Optional<OrderJpaEntity> findWithDetailsById(@Param("id") Long id);

    Optional<OrderJpaEntity> findByIdAndPaymentKey(Long orderId, String paymentKey);

    @Query(
            value = """
                    SELECT
                        o.id AS id,
                        o.customer_id AS customerId,
                        o.order_date AS orderDate,
                        o.observations AS observations,
                        o.status AS status,
                        o.total AS total,
                        (
                            SELECT COUNT(*)
                            FROM order_items oi
                            WHERE oi.order_id = o.id
                        ) AS amountItems
                    FROM orders o
                    WHERE o.id = :orderId
                    """,
            nativeQuery = true
    )
    Optional<OrderSummaryJpaProjection> findSummaryById(@Param("orderId") Long orderId);
}
