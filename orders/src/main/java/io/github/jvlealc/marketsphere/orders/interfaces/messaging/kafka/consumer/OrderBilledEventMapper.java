package io.github.jvlealc.marketsphere.orders.interfaces.messaging.kafka.consumer;

import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderBilledCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderBilledEventMapper {

    HandleOrderBilledCommand toCommand(OrderBilledEvent event);
}
