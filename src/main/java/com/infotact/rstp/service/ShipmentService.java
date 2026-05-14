package com.infotact.rstp.service;

import com.infotact.rstp.dto.ShipmentRequest;
import com.infotact.rstp.dto.ShipmentResponse;
import com.infotact.rstp.entity.ShipmentStatus;

import java.util.List;

public interface ShipmentService {
    ShipmentResponse createShipment(ShipmentRequest request);
    ShipmentResponse assignCarrier(Long shipmentId, Long carrierId);
    List<ShipmentResponse> getAllShipments();
    List<ShipmentResponse> getAvailableShipments();
    ShipmentResponse getShipmentById(Long id);
    ShipmentResponse updateShipment(Long id, ShipmentRequest request);
    ShipmentResponse updateShipmentStatus(Long shipmentId, Long carrierId, ShipmentStatus status);
    void deleteShipment(Long id);
}
