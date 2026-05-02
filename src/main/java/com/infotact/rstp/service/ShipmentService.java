package com.infotact.rstp.service;

import com.infotact.rstp.dto.ShipmentRequest;
import com.infotact.rstp.dto.ShipmentResponse;

import java.util.List;

public interface ShipmentService {
    ShipmentResponse createShipment(ShipmentRequest request);
    List<ShipmentResponse> getAllShipments();
    ShipmentResponse getShipmentById(Long id);
    ShipmentResponse updateShipment(Long id, ShipmentRequest request);
    void deleteShipment(Long id);
}
