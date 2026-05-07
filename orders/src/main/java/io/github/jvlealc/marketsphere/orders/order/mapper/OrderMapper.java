package io.github.jvlealc.marketsphere.orders.order.mapper;

import io.github.jvlealc.marketsphere.orders.client.products.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.order.dto.OrderRequestDto;
import io.github.jvlealc.marketsphere.orders.order.dto.OrderResponseDto;
import io.github.jvlealc.marketsphere.orders.order.model.Order;
import io.github.jvlealc.marketsphere.orders.order.model.enums.OrderStatus;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Mapper(
        componentModel = "spring",
        uses = { OrderItemMapper.class }
)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderDate", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "billedAt", ignore = true)
    @Mapping(target = "shippedAt", ignore = true)
    @Mapping(target = "paymentKey", ignore = true)
    @Mapping(target = "observations", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "trackingCode", ignore = true)
    @Mapping(target = "invoiceUrl", ignore = true)
    Order toOrderEntity(OrderRequestDto orderRequestDto, @Context Map<Long, ProductRepresentation> productsMap);

    @Mapping(target = "amountItems", expression = "java(order.getOrderItems() != null ? order.getOrderItems().size() : 0)")
    OrderResponseDto toOrderDto(Order order);

    @AfterMapping
    default void afterMapping(@MappingTarget Order order) {
        order.setStatus(OrderStatus.PLACED);
        order.setOrderDate(Instant.now());
        var total = calculateTotalPrice(order);
        order.setTotal(total);
        order.getOrderItems().forEach(item -> item.setOrder(order));
    }

    private static BigDecimal calculateTotalPrice(Order order) {
        return order.getOrderItems()
                .stream()
                .map(item ->
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getAmount()))
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
    }
}
