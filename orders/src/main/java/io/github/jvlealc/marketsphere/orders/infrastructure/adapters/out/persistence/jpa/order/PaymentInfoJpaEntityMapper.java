package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.persistence.jpa.order;

import io.github.jvlealc.marketsphere.orders.domain.model.vo.PaymentInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentInfoJpaEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    PaymentInfoJpaEntity toNewEntity(PaymentInfo paymentInfo);

    default PaymentInfo toDomain(PaymentInfoJpaEntity entity) {
        if (entity == null) return null;
        return PaymentInfo.rehydrate(entity.getMetadata(), entity.getPaymentType(), entity.getCreatedAt());
    }
}
