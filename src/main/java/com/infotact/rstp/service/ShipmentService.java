package com.infotact.rstp.service;

import com.infotact.rstp.dto.ShipmentRequest;
import com.infotact.rstp.dto.ShipmentResponse;
import com.infotact.rstp.entity.ShipmentStatus;

import java.util.List;

public interface ShipmentService {
    ShipmentResponse createShipment(ShipmentRequest request, String shipperEmail);
    ShipmentResponse assignCarrier(Long shipmentId, Long carrierId, String shipperEmail);
    List<ShipmentResponse> getShipmentsForUser(String userEmail);
    List<ShipmentResponse> getAvailableShipments();
    ShipmentResponse getShipmentById(Long id, String userEmail);
    ShipmentResponse getPublicShipmentById(Long id);
    ShipmentResponse updateShipment(Long id, ShipmentRequest request, String shipperEmail);
    ShipmentResponse updateShipmentStatus(Long shipmentId, ShipmentStatus status, String carrierEmail);
    void deleteShipment(Long id, String shipperEmail);
}
