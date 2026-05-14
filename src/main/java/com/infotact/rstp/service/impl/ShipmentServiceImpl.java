package com.infotact.rstp.service.impl;

import com.infotact.rstp.dto.ShipmentRequest;
import com.infotact.rstp.dto.ShipmentResponse;
import com.infotact.rstp.entity.NotificationType;
import com.infotact.rstp.entity.Role;
import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.ShipmentStatus;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.exception.ResourceNotFoundException;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.UserRepository;
import com.infotact.rstp.service.NotificationService;
import com.infotact.rstp.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public ShipmentResponse createShipment(ShipmentRequest request, String shipperEmail) {
        User shipper = getUserByEmail(shipperEmail);

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

    @Override
    public ShipmentResponse assignCarrier(Long shipmentId, Long carrierId, String shipperEmail) {
        Shipment shipment = getShipment(shipmentId);
        assertShipperOwnsShipment(shipment, shipperEmail);

        User carrier = userRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with id: " + carrierId));

        if (carrier.getRole() != Role.CARRIER) {
            throw new IllegalArgumentException("User is not a CARRIER");
        }

        shipment.setAwardedCarrier(carrier);
        shipment.setStatus(ShipmentStatus.AWAITING_PICKUP);
        shipment.setEstimatedDeliveryAt(LocalDateTime.now().plusDays(1));

        return mapToResponse(shipmentRepository.save(shipment));
    }

    @Override
    public List<ShipmentResponse> getShipmentsForUser(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Shipment> shipments;

        if (user.getRole() == Role.SHIPPER) {
            shipments = shipmentRepository.findByShipperId(user.getId());
        } else {
            List<Shipment> marketplaceLoads = shipmentRepository.findByStatusIn(List.of(
                    ShipmentStatus.OPEN,
                    ShipmentStatus.BIDDING
            ));
            List<Shipment> assignedLoads = shipmentRepository.findByAwardedCarrierId(user.getId());
            LinkedHashMap<Long, Shipment> merged = new LinkedHashMap<>();
            new ArrayList<>(marketplaceLoads).forEach(shipment -> merged.put(shipment.getShipmentId(), shipment));
            assignedLoads.forEach(shipment -> merged.put(shipment.getShipmentId(), shipment));
            shipments = new ArrayList<>(merged.values());
        }

        return shipments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteShipment(Long id, String shipperEmail) {
        Shipment shipment = getShipment(id);
        assertShipperOwnsShipment(shipment, shipperEmail);
        shipmentRepository.delete(shipment);
    }

    @Override
    public ShipmentResponse getShipmentById(Long shipmentId, String userEmail) {
        Shipment shipment = getShipment(shipmentId);
        assertUserCanViewShipment(shipment, userEmail);
        return mapToResponse(shipment);
    }

    @Override
    public ShipmentResponse getPublicShipmentById(Long shipmentId) {
        return mapToResponse(getShipment(shipmentId));
    }

    @Override
    public ShipmentResponse updateShipment(Long shipmentId, ShipmentRequest request, String shipperEmail) {
        Shipment shipment = getShipment(shipmentId);
        assertShipperOwnsShipment(shipment, shipperEmail);

        shipment.setTitle(request.getTitle());
        shipment.setDescription(request.getDescription());
        shipment.setOrigin(request.getOrigin());
        shipment.setDestination(request.getDestination());
        shipment.setWeight(request.getWeight());
        shipment.setPriceExpected(request.getPriceExpected());

        return mapToResponse(shipmentRepository.save(shipment));
    }

    @Override
    public ShipmentResponse updateShipmentStatus(Long shipmentId, ShipmentStatus status, String carrierEmail) {
        Shipment shipment = getShipment(shipmentId);
        User carrier = getUserByEmail(carrierEmail);

        if (carrier.getRole() != Role.CARRIER) {
            throw new IllegalArgumentException("User is not a CARRIER");
        }

        if (shipment.getAwardedCarrier() == null ||
                !shipment.getAwardedCarrier().getId().equals(carrier.getId())) {
            throw new IllegalArgumentException("Carrier is not awarded to this shipment");
        }

        validateStatusTransition(shipment.getStatus(), status);
        shipment.setStatus(status);

        if (status == ShipmentStatus.IN_TRANSIT) {
            shipment.setEstimatedDeliveryAt(LocalDateTime.now().plusHours(12));
            notificationService.createAndBroadcastNotification(
                    shipment.getShipper().getId(),
                    shipment.getShipmentId(),
                    "Shipment #" + shipment.getShipmentId() + " is in transit. Estimated delivery: "
                            + shipment.getEstimatedDeliveryAt(),
                    NotificationType.SHIPMENT_PICKED_UP
            );
        } else if (status == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(LocalDateTime.now());
            notificationService.createAndBroadcastNotification(
                    shipment.getShipper().getId(),
                    shipment.getShipmentId(),
                    "Shipment #" + shipment.getShipmentId() + " has been delivered.",
                    NotificationType.SHIPMENT_DELIVERED
            );
        }

        return mapToResponse(shipmentRepository.save(shipment));
    }

    private void validateStatusTransition(ShipmentStatus currentStatus, ShipmentStatus newStatus) {
        if (currentStatus == ShipmentStatus.AWAITING_PICKUP && newStatus == ShipmentStatus.IN_TRANSIT) {
            return;
        }

        if (currentStatus == ShipmentStatus.IN_TRANSIT && newStatus == ShipmentStatus.DELIVERED) {
            return;
        }

        throw new IllegalArgumentException(
                "Invalid shipment status transition from " + currentStatus + " to " + newStatus
        );
    }

    @Override
    public List<ShipmentResponse> getAvailableShipments() {
        return shipmentRepository.findByStatus(ShipmentStatus.OPEN)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Shipment getShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + shipmentId));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void assertShipperOwnsShipment(Shipment shipment, String shipperEmail) {
        User shipper = getUserByEmail(shipperEmail);
        if (shipper.getRole() != Role.SHIPPER || shipment.getShipper() == null ||
                !shipment.getShipper().getId().equals(shipper.getId())) {
            throw new AccessDeniedException("You can only manage your own shipments");
        }
    }

    private void assertUserCanViewShipment(Shipment shipment, String userEmail) {
        User user = getUserByEmail(userEmail);
        boolean isOwner = shipment.getShipper() != null && shipment.getShipper().getId().equals(user.getId());
        boolean isAwardedCarrier = shipment.getAwardedCarrier() != null &&
                shipment.getAwardedCarrier().getId().equals(user.getId());
        boolean isCarrierViewingMarketplaceLoad = user.getRole() == Role.CARRIER &&
                (shipment.getStatus() == ShipmentStatus.OPEN || shipment.getStatus() == ShipmentStatus.BIDDING);

        if (!isOwner && !isAwardedCarrier && !isCarrierViewingMarketplaceLoad) {
            throw new AccessDeniedException("You cannot view this shipment");
        }
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
                .acceptedBidAmount(shipment.getAcceptedBidAmount())
                .estimatedDeliveryAt(shipment.getEstimatedDeliveryAt())
                .deliveredAt(shipment.getDeliveredAt())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
