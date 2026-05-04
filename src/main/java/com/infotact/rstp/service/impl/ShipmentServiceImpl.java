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
import com.infotact.rstp.exception.InvalidShipmentStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository, UserRepository userRepository) {
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ShipmentResponse createShipment(ShipmentRequest request) {
        User shipper = userRepository.findById(request.getShipperId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipper not found with id: " + request.getShipperId()));

        if (shipper.getRole() != Role.SHIPPER) {
            throw new IllegalArgumentException("Only SHIPPER users can create shipments");
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

    @Override
    public List<ShipmentResponse> getAllShipments() {
        return shipmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ShipmentResponse getShipmentById(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + id));
        return mapToResponse(shipment);
    }

    @Override
    public ShipmentResponse updateShipment(Long id, ShipmentRequest request) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + id));

        User shipper = userRepository.findById(request.getShipperId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipper not found with id: " + request.getShipperId()));

        if (shipper.getRole() != Role.SHIPPER) {
            throw new IllegalArgumentException("Only SHIPPER users can own shipments");
        }

        shipment.setTitle(request.getTitle());
        shipment.setDescription(request.getDescription());
        shipment.setOrigin(request.getOrigin());
        shipment.setDestination(request.getDestination());
        shipment.setWeight(request.getWeight());
        shipment.setPriceExpected(request.getPriceExpected());
        shipment.setShipper(shipper);

        return mapToResponse(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional
    public ShipmentResponse updateShipmentStatus(Long shipmentId, Long carrierId, ShipmentStatus newStatus) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + shipmentId));

        User carrier = userRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with id: " + carrierId));

        if (carrier.getRole() != Role.CARRIER) {
            throw new IllegalArgumentException("Only CARRIER users can update shipment status");
        }

        if (shipment.getAwardedCarrier() == null ||
                !shipment.getAwardedCarrier().getId().equals(carrierId)) {
            throw new IllegalArgumentException("Carrier is not awarded to this shipment");
        }

        validateStatusTransition(shipment.getStatus(), newStatus);

        shipment.setStatus(newStatus);

        return mapToResponse(shipmentRepository.save(shipment));
    }

    private void validateStatusTransition(ShipmentStatus currentStatus, ShipmentStatus newStatus) {
        boolean validTransition =
                (currentStatus == ShipmentStatus.AWARDED && newStatus == ShipmentStatus.AWAITING_PICKUP) ||
                (currentStatus == ShipmentStatus.AWAITING_PICKUP && newStatus == ShipmentStatus.IN_TRANSIT) ||
                (currentStatus == ShipmentStatus.IN_TRANSIT && newStatus == ShipmentStatus.DELIVERED);

        if (!validTransition) {
            throw new InvalidShipmentStatusException(
                    "Invalid shipment status transition from " + currentStatus + " to " + newStatus
            );
        }
    }

    @Override
    public void deleteShipment(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + id));
        shipmentRepository.delete(shipment);
    }

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
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
