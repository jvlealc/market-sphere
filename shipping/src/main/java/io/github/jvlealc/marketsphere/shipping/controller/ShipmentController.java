package io.github.jvlealc.marketsphere.shipping.controller;

import io.github.jvlealc.marketsphere.shipping.dto.DispatchShipmentRequest;
import io.github.jvlealc.marketsphere.shipping.service.ShipmentDispatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentDispatchService shipmentDispatchService;

    public ShipmentController(ShipmentDispatchService shipmentDispatchService) {
        this.shipmentDispatchService = shipmentDispatchService;
    }

    @PostMapping("/dispatch")
    public ResponseEntity<Void> dispatchShipment(@RequestBody @Valid DispatchShipmentRequest request) {
        shipmentDispatchService.dispatch(request);
        return ResponseEntity.noContent().build();
    }
}
