package io.github.jvlealc.marketsphere.orders.controller;

import io.github.jvlealc.marketsphere.orders.controller.util.HeaderLocationBuilder;
import io.github.jvlealc.marketsphere.orders.dto.OrderRequestDto;
import io.github.jvlealc.marketsphere.orders.dto.PaymentInfoRequestDto;
import io.github.jvlealc.marketsphere.orders.service.OrderService;
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

    private final OrderService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createOrder(@RequestBody @Valid OrderRequestDto orderRequestDto) {
        return ResponseEntity
                .created(HeaderLocationBuilder.build(
                        service.createOrder(orderRequestDto).id()
                ))
                .build();
    }

    @PostMapping(value = "/{orderId}/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createPayment(
            @PathVariable
            @Positive(message = "{order.id.positive}")
            @NotNull(message = "{order.id.required}")
            Long orderId,

            @RequestBody
            @Valid
            PaymentInfoRequestDto paymentRequestDto
    ) {
        service.initiatePayment(
                orderId,
                paymentRequestDto.metadata(),
                paymentRequestDto.paymentType()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getOrderById(
            @PathVariable
            @Positive(message = "{order.id.positive}")
            @NotNull(message = "{order.id.required}")
            Long orderId,

            @RequestParam(value = "view", defaultValue = "summary")
            String view
    ) {
        if ("details".equalsIgnoreCase(view)) {
            return ResponseEntity.ok(service.getOrderDetailsById(orderId));
        }
        return ResponseEntity.ok(service.getOrderById(orderId));
    }
}
