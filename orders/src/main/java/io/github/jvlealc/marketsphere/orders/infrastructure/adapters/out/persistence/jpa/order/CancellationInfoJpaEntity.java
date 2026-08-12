package io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity;

import io.github.jvlealc.marketsphere.orders.domain.model.enums.CancellationInitiator;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "canceled_orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = "order")
public class CancellationInfoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private OrderJpaEntity order;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_initiator", nullable = false, length = 30)
    private CancellationInitiator initiator;

    @Column(name = "canceled_at",  nullable = false, updatable = false)
    private Instant canceledAt;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CancellationInfoJpaEntity other = (CancellationInfoJpaEntity) obj;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}