package com.infotact.rstp.service;

import com.infotact.rstp.dto.TrackingEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulatedDriverService {

    private final TrackingService trackingService;

    // A mock active shipment ID. In a real scenario, this service would
    // query the database for all shipments currently IN_TRANSIT.
    private static final Long MOCK_ACTIVE_SHIPMENT_ID = 1L;
    private static final Long MOCK_CARRIER_ID = 2L;

    // Starting coordinates (e.g., somewhere in New York)
    private double currentLat = 40.7128;
    private double currentLng = -74.0060;

    /**
     * This scheduled task runs every 10 seconds.
     * It simulates a carrier driving by slightly adjusting the GPS coordinates
     * and pushing the new location to the TrackingService (which broadcasts it via WebSocket).
     */
    @Scheduled(fixedRate = 10000)
    public void simulateDriving() {
        // Only simulate if we have a reason to (you can toggle this off for tests)
        boolean isSimulationEnabled = true;

        if (isSimulationEnabled) {
            // Simulate movement: adjust coordinates slightly
            currentLat += (Math.random() - 0.5) * 0.01;
            currentLng += (Math.random() - 0.5) * 0.01;

            TrackingEventDTO mockEvent = TrackingEventDTO.builder()
                    .shipmentId(MOCK_ACTIVE_SHIPMENT_ID)
                    .carrierId(MOCK_CARRIER_ID)
                    .latitude(currentLat)
                    .longitude(currentLng)
                    .locationDesc("Simulated Highway " + (int)(Math.random() * 100))
                    .eventType("LOCATION_UPDATE")
                    .notes("Driver is en route (SIMULATION)")
                    .eventTimestamp(LocalDateTime.now())
                    .build();

            try {
                // We use recordAndBroadcastEvent which saves to DB and broadcasts via WebSockets
                trackingService.recordAndBroadcastEvent(mockEvent);
                log.debug("Simulated GPS Ping sent for shipment {}: [{}, {}]", 
                          MOCK_ACTIVE_SHIPMENT_ID, currentLat, currentLng);
            } catch (Exception e) {
                // If the shipment doesn't exist yet, just ignore the error during simulation
                log.trace("Could not save simulated ping (Shipment might not exist yet)");
            }
        }
    }
}
