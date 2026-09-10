package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.kafka.billing;

import io.github.jvlealc.marketsphere.orders.application.order.billing.HandleOrderBilledCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderBilledEventMapper {

    HandleOrderBilledCommand toCommand(OrderBilledEvent event);
}
