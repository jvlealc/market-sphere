package io.github.jvlealc.marketsphere.orders.infrastructure.persistence.mapper;

import io.github.jvlealc.marketsphere.orders.domain.model.vo.CancellationInfo;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.CancellationInfoJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

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
