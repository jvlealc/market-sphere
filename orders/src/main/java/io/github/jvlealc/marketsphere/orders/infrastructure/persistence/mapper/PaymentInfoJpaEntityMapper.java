package io.github.jvlealc.marketsphere.orders.infrastructure.persistence.mapper;

import io.github.jvlealc.marketsphere.orders.domain.model.vo.PaymentInfo;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.PaymentInfoJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

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
