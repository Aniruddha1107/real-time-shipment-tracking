package com.infotact.rstp.controller;

import com.infotact.rstp.dto.TrackingEventDTO;
import com.infotact.rstp.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    // WebSocket STOMP endpoint: /app/tracking.update
    @MessageMapping("/tracking.update")
    public void processTrackingUpdate(@Payload TrackingEventDTO eventDto) {
        // Receives the live ping from the carrier's device and processes it
        trackingService.recordAndBroadcastEvent(eventDto);
    }

    // Standard REST API to fetch historical tracking data
    @GetMapping("/{shipmentId}")
    public ResponseEntity<List<TrackingEventDTO>> getTrackingHistory(@PathVariable Long shipmentId) {
        List<TrackingEventDTO> history = trackingService.getTrackingHistory(shipmentId);
        return ResponseEntity.ok(history);
    }
}
