package io.github.jvlealc.marketsphere.orders.mapper;

import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.dto.OrderItemRequestDto;
import io.github.jvlealc.marketsphere.orders.mapper.provider.ProductPriceHelper;
import io.github.jvlealc.marketsphere.orders.model.OrderItem;
import org.mapstruct.Context;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

@Mapper(
        componentModel = "spring",
        uses = { ProductPriceHelper.class },
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface OrderItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "unitPrice", source = "productId", qualifiedByName = "fetchProductUnitPrice")
    OrderItem toOrderItemEntity(OrderItemRequestDto orderItemRequestDto, @Context Map<Long, ProductRepresentation> productsMap);
}
