package com.infotact.rstp.service.impl;

import com.infotact.rstp.dto.ShipmentRequest;
import com.infotact.rstp.dto.ShipmentResponse;
import com.infotact.rstp.entity.Role;
import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.ShipmentStatus;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.exception.ResourceNotFoundException;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.UserRepository;
import com.infotact.rstp.service.ShipmentService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    // ✅ CREATE SHIPMENT
    @Override
    public ShipmentResponse createShipment(ShipmentRequest request) {

        User shipper = userRepository.findById(request.getShipperId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Shipper not found")
                );

        if (shipper.getRole() != Role.SHIPPER) {
            throw new IllegalArgumentException("Only SHIPPER users allowed");
        }

        Shipment shipment = Shipment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .weight(request.getWeight())
                .priceExpected(request.getPriceExpected())
                .status(ShipmentStatus.OPEN)
                .shipper(shipper)
                .build();

        return mapToResponse(shipmentRepository.save(shipment));
    }

    // ✅ ASSIGN CARRIER
    @Override
    public ShipmentResponse assignCarrier(Long shipmentId, Long carrierId) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Shipment not found with id: " + shipmentId)
                );

        User carrier = userRepository.findById(carrierId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Carrier not found with id: " + carrierId)
                );

        if (carrier.getRole() != Role.CARRIER) {
            throw new IllegalArgumentException("User is not a CARRIER");
        }

        shipment.setAwardedCarrier(carrier);
        shipment.setStatus(ShipmentStatus.AWARDED);

        return mapToResponse(shipmentRepository.save(shipment));
    }

    // ✅ GET ALL SHIPMENTS
    @Override
    public List<ShipmentResponse> getAllShipments() {
        return shipmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ✅ DELETE SHIPMENT
    @Override
    public void deleteShipment(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Shipment not found with id: " + id)
                );

        shipmentRepository.delete(shipment);
    }

    // ✅ RESPONSE MAPPER
    private ShipmentResponse mapToResponse(Shipment shipment) {

        return ShipmentResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .title(shipment.getTitle())
                .description(shipment.getDescription())
                .origin(shipment.getOrigin())
                .destination(shipment.getDestination())
                .weight(shipment.getWeight())
                .priceExpected(shipment.getPriceExpected())
                .status(shipment.getStatus())
                .shipperId(shipment.getShipper() != null ? shipment.getShipper().getId() : null)
                .shipperName(shipment.getShipper() != null ? shipment.getShipper().getName() : null)
                .awardedCarrierId(shipment.getAwardedCarrier() != null ? shipment.getAwardedCarrier().getId() : null)
                .awardedCarrierName(shipment.getAwardedCarrier() != null ? shipment.getAwardedCarrier().getName() : null)
                .createdAt(shipment.getCreatedAt())
                .build();
    }
}