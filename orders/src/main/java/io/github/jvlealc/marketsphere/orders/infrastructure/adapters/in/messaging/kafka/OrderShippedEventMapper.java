package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.messaging.kafka;

import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderShippedCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderShippedEventMapper {

    HandleOrderShippedCommand toCommand(OrderShippedEvent event);
}
