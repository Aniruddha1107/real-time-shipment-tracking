package com.infotact.rstp.controller;

import com.infotact.rstp.dto.ShipmentResponse;
import com.infotact.rstp.dto.TrackingEventDTO;
import com.infotact.rstp.service.ShipmentService;
import com.infotact.rstp.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/tracking")
@RequiredArgsConstructor
public class PublicTrackingController {

    private final ShipmentService shipmentService;
    private final TrackingService trackingService;

    @GetMapping("/{shipmentId}/shipment")
    public ResponseEntity<ShipmentResponse> getShipment(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(shipmentService.getPublicShipmentById(shipmentId));
    }

    @GetMapping("/{shipmentId}/history")
    public ResponseEntity<List<TrackingEventDTO>> getHistory(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(trackingService.getPublicTrackingHistory(shipmentId));
    }
}
