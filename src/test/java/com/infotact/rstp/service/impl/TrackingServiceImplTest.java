package com.infotact.rstp.service.impl;

import com.infotact.rstp.dto.TrackingEventDTO;
import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.TrackingEvent;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.TrackingEventRepository;
import com.infotact.rstp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingServiceImplTest {

    @Mock
    private TrackingEventRepository trackingEventRepository;
    
    @Mock
    private ShipmentRepository shipmentRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    private Shipment mockShipment;
    private User mockCarrier;
    private TrackingEventDTO mockDto;
    private TrackingEvent mockEvent;

    @BeforeEach
    void setUp() {
        mockShipment = new Shipment();
    	mockShipment.setShipmentId(1L);

    	mockCarrier = new User();
    	mockCarrier.setId(2L);

    	mockShipment.setAwardedCarrier(mockCarrier);

        mockDto = TrackingEventDTO.builder()
                .shipmentId(1L)
                .carrierId(2L)
                .latitude(40.7128)
                .longitude(-74.0060)
                .locationDesc("Test Location")
                .eventType("LOCATION_UPDATE")
                .notes("All good")
                .eventTimestamp(LocalDateTime.now())
                .build();

        mockEvent = TrackingEvent.builder()
                .id(10L)
                .shipment(mockShipment)
                .carrier(mockCarrier)
                .latitude(40.7128)
                .longitude(-74.0060)
                .locationDesc("Test Location")
                .eventType("LOCATION_UPDATE")
                .notes("All good")
                .eventTimestamp(mockDto.getEventTimestamp())
                .build();
    }

    @Test
    void testRecordAndBroadcastEvent_Success() {
        // Arrange
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(mockShipment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockCarrier));
        when(trackingEventRepository.save(any(TrackingEvent.class))).thenReturn(mockEvent);

        // Act
        TrackingEventDTO result = trackingService.recordAndBroadcastEvent(mockDto);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Test Location", result.getLocationDesc());

        // Verify DB Save
        verify(trackingEventRepository).save(any(TrackingEvent.class));

        // Verify WebSocket Broadcast
        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/1"), any(TrackingEventDTO.class));
    }

    @Test
    void testGetTrackingHistory_ReturnsList() {
        // Arrange
        when(trackingEventRepository.findByShipment_ShipmentIdOrderByEventTimestampDesc(1L))
                .thenReturn(List.of(mockEvent));

        // Act
        List<TrackingEventDTO> history = trackingService.getTrackingHistory(1L);

        // Assert
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("Test Location", history.get(0).getLocationDesc());
        assertEquals(40.7128, history.get(0).getLatitude());
    }
}
