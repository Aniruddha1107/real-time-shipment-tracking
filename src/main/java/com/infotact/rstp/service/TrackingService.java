package com.infotact.rstp.service;

import java.util.List;

import com.infotact.rstp.dto.TrackingEventDTO;

public interface TrackingService {
    TrackingEventDTO recordAndBroadcastEvent(TrackingEventDTO eventDto);
    List<TrackingEventDTO> getTrackingHistory(Long shipmentId);
}
