package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.kafka.shipping;

import io.github.jvlealc.marketsphere.orders.application.order.shipping.HandleOrderShippedCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderShippedEventMapper {

    HandleOrderShippedCommand toCommand(OrderShippedEvent event);
}
