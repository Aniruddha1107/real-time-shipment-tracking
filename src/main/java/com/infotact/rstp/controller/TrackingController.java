package com.infotact.rstp.controller;

import com.infotact.rstp.dto.TrackingEventDTO;
import com.infotact.rstp.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @MessageMapping("/tracking.update")
    public void processTrackingUpdate(@Payload TrackingEventDTO eventDto, Principal principal) {
        if (principal != null) {
            trackingService.recordAndBroadcastEvent(eventDto, principal.getName());
        } else {
            trackingService.recordAndBroadcastEvent(eventDto);
        }
    }

    @GetMapping("/{shipmentId}")
    public ResponseEntity<List<TrackingEventDTO>> getTrackingHistory(@PathVariable Long shipmentId,
                                                                     Authentication authentication) {
        List<TrackingEventDTO> history = trackingService.getTrackingHistory(shipmentId, authentication.getName());
        return ResponseEntity.ok(history);
    }
}
