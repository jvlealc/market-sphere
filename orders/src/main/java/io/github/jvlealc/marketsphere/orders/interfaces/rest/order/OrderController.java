package io.github.jvlealc.marketsphere.orders.interfaces.rest.order;

import io.github.jvlealc.marketsphere.orders.application.output.OrderDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.output.OrderSummaryOutput;
import io.github.jvlealc.marketsphere.orders.application.usecase.GetOrderDetailsUseCase;
import io.github.jvlealc.marketsphere.orders.application.usecase.GetOrderSummaryUseCase;
import io.github.jvlealc.marketsphere.orders.application.usecase.PlaceOrderUseCase;
import io.github.jvlealc.marketsphere.orders.interfaces.rest.common.support.HeaderLocationBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrderDetailsUseCase getOrderDetailsUseCase;
    private final GetOrderSummaryUseCase getOrderSummaryUseCase;
    private final OrderRestMapper orderRestMapper;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> placeOrder(@RequestBody @Valid PlaceOrderRequest request) {
        Long orderId = placeOrderUseCase.execute(
                orderRestMapper.toPlaceOrderCommand(request)
        );
        return ResponseEntity.created(
                HeaderLocationBuilder.build(orderId)
        ).build();
    }

    @GetMapping(value = "/{orderId}/details", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderDetailsResponse> getOrderDetailsById(
            @PathVariable
            @Positive(message = "{order.customerId.positive}")
            @NotNull(message = "{order.customerId.required}")
            Long orderId
    ) {
        OrderDetailsOutput output = getOrderDetailsUseCase.execute(
                orderRestMapper.toDetailsQuery(orderId)
        );
        return ResponseEntity.ok(
                orderRestMapper.toDetailsResponse(output)
        );
    }

    @GetMapping(value = "/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderSummaryResponse> getOrderSummaryById(
            @PathVariable
            @Positive(message = "{order.customerId.positive}")
            @NotNull(message = "{order.customerId.required}")
            Long orderId
    ) {
        OrderSummaryOutput output = getOrderSummaryUseCase.execute(
                orderRestMapper.toSummaryQuery(orderId)
        );
        return ResponseEntity.ok(
                orderRestMapper.toSummaryResponse(output)
        );
    }
}
