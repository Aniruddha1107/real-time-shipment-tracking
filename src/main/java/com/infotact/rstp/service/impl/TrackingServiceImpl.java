package com.infotact.rstp.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infotact.rstp.dto.TrackingEventDTO;
import com.infotact.rstp.entity.Role;
import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.TrackingEvent;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.TrackingEventRepository;
import com.infotact.rstp.repository.UserRepository;
import com.infotact.rstp.service.TrackingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public TrackingEventDTO recordAndBroadcastEvent(TrackingEventDTO eventDto) {
        return recordAndBroadcast(eventDto, null);
    }

    @Override
    @Transactional
    public TrackingEventDTO recordAndBroadcastEvent(TrackingEventDTO eventDto, String carrierEmail) {
        User authenticatedCarrier = userRepository.findByEmail(carrierEmail)
                .orElseThrow(() -> new IllegalArgumentException("Carrier not found"));
        if (eventDto.getCarrierId() != null && !eventDto.getCarrierId().equals(authenticatedCarrier.getId())) {
            throw new IllegalArgumentException("Tracking carrier does not match authenticated user");
        }
        eventDto.setCarrierId(authenticatedCarrier.getId());
        return recordAndBroadcast(eventDto, authenticatedCarrier);
    }

    private TrackingEventDTO recordAndBroadcast(TrackingEventDTO eventDto, User authenticatedCarrier) {
        Shipment shipment = shipmentRepository.findById(eventDto.getShipmentId())
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        User carrier = authenticatedCarrier != null ? authenticatedCarrier : userRepository.findById(eventDto.getCarrierId())
                .orElseThrow(() -> new IllegalArgumentException("Carrier not found"));

        if (carrier.getRole() != null && carrier.getRole() != Role.CARRIER) {
            throw new IllegalArgumentException("Only carriers can send tracking updates");
        }

        if (shipment.getAwardedCarrier() == null ||
                !shipment.getAwardedCarrier().getId().equals(carrier.getId())) {
            throw new IllegalArgumentException("Carrier is not awarded to this shipment");
        }

        TrackingEvent event = TrackingEvent.builder()
                .shipment(shipment)
                .carrier(carrier)
                .latitude(eventDto.getLatitude())
                .longitude(eventDto.getLongitude())
                .locationDesc(eventDto.getLocationDesc())
                .eventType(eventDto.getEventType() != null ? eventDto.getEventType() : "LOCATION_UPDATE")
                .notes(eventDto.getNotes())
                .build();

        TrackingEvent savedEvent = trackingEventRepository.save(event);
        
        TrackingEventDTO savedDto = mapToDTO(savedEvent);

        // Broadcast to the specific shipment's tracking topic
        messagingTemplate.convertAndSend("/topic/tracking/" + shipment.getShipmentId(), savedDto);

        return savedDto;
    }

    @Override
    public List<TrackingEventDTO> getTrackingHistory(Long shipmentId, String userEmail) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean canView = shipment.getShipper() != null && shipment.getShipper().getId().equals(user.getId());
        canView = canView || shipment.getAwardedCarrier() != null &&
                shipment.getAwardedCarrier().getId().equals(user.getId());
        if (!canView) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot view this tracking history");
        }

        return getPublicTrackingHistory(shipmentId);
    }

    @Override
    public List<TrackingEventDTO> getPublicTrackingHistory(Long shipmentId) {
        return trackingEventRepository.findByShipment_ShipmentIdOrderByEventTimestampDesc(shipmentId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TrackingEventDTO mapToDTO(TrackingEvent event) {
        return TrackingEventDTO.builder()
                .id(event.getId())
                .shipmentId(event.getShipment().getShipmentId())
                .carrierId(event.getCarrier().getId())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .locationDesc(event.getLocationDesc())
                .eventType(event.getEventType())
                .notes(event.getNotes())
                .eventTimestamp(event.getEventTimestamp())
                .build();
    }
}
