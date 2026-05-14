package com.infotact.rstp.controller;

import com.infotact.rstp.dto.ShipmentRequest;
import com.infotact.rstp.dto.ShipmentResponse;
import com.infotact.rstp.dto.ShipmentStatusUpdateRequest;
import com.infotact.rstp.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PreAuthorize("hasRole('SHIPPER')")
    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody ShipmentRequest request,
                                                           Authentication authentication) {
        return new ResponseEntity<>(
                shipmentService.createShipment(request, authentication.getName()),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getAllShipments(Authentication authentication) {
        return ResponseEntity.ok(shipmentService.getShipmentsForUser(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable Long id,
                                                            Authentication authentication) {
        return ResponseEntity.ok(shipmentService.getShipmentById(id, authentication.getName()));
    }

    @PreAuthorize("hasRole('SHIPPER')")
    @PutMapping("/{shipmentId}/assign/{carrierId}")
    public ResponseEntity<ShipmentResponse> assignCarrier(
            @PathVariable Long shipmentId,
            @PathVariable Long carrierId,
            Authentication authentication) {

        return ResponseEntity.ok(
                shipmentService.assignCarrier(shipmentId, carrierId, authentication.getName())
        );
    }

    @PreAuthorize("hasRole('CARRIER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                shipmentService.updateShipmentStatus(id, request.getStatus(), authentication.getName())
        );
    }

    @PreAuthorize("hasRole('CARRIER')")
    @GetMapping("/available")
    public ResponseEntity<List<ShipmentResponse>> getAvailableShipments() {
        return ResponseEntity.ok(shipmentService.getAvailableShipments());
    }

    @PreAuthorize("hasRole('SHIPPER')")
    @PutMapping("/{id}")
    public ResponseEntity<ShipmentResponse> updateShipment(@PathVariable Long id,
                                                           @Valid @RequestBody ShipmentRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(shipmentService.updateShipment(id, request, authentication.getName()));
    }

    @PreAuthorize("hasRole('SHIPPER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteShipment(@PathVariable Long id,
                                                 Authentication authentication) {
        shipmentService.deleteShipment(id, authentication.getName());
        return ResponseEntity.ok("Shipment deleted successfully");
    }
}
