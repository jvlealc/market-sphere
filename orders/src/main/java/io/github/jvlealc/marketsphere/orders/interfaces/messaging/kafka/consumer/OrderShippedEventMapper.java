package io.github.jvlealc.marketsphere.orders.interfaces.messaging.kafka.consumer;

import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderShippedCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderShippedEventMapper {

    HandleOrderShippedCommand toCommand(OrderShippedEvent event);
}
