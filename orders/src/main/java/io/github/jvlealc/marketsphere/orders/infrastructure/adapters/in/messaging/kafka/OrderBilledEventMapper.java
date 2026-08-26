package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.messaging.kafka;

import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderBilledCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderBilledEventMapper {

    HandleOrderBilledCommand toCommand(OrderBilledEvent event);
}
