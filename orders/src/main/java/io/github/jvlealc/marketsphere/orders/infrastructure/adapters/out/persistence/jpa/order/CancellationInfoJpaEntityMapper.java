package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.persistence.jpa.order;

import io.github.jvlealc.marketsphere.orders.domain.model.vo.CancellationInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CancellationInfoJpaEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    CancellationInfoJpaEntity toNewEntity(CancellationInfo cancellationInfo);

    default CancellationInfo toDomain(CancellationInfoJpaEntity entity) {
        if (entity == null) return null;
        return CancellationInfo.rehydrate(entity.getInitiator(), entity.getReason(), entity.getCanceledAt());
    }
}
