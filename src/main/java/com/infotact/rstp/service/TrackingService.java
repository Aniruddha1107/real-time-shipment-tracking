package com.infotact.rstp.service;

import java.util.List;

import com.infotact.rstp.dto.TrackingEventDTO;

public interface TrackingService {
    TrackingEventDTO recordAndBroadcastEvent(TrackingEventDTO eventDto);
    TrackingEventDTO recordAndBroadcastEvent(TrackingEventDTO eventDto, String carrierEmail);
    List<TrackingEventDTO> getTrackingHistory(Long shipmentId, String userEmail);
    List<TrackingEventDTO> getPublicTrackingHistory(Long shipmentId);
}
