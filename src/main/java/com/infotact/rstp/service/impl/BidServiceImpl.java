package com.infotact.rstp.service.impl;

import com.infotact.rstp.dto.BidRequest;
import com.infotact.rstp.dto.BidResponse;
import com.infotact.rstp.entity.Bid;
import com.infotact.rstp.entity.BidStatus;
import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.ShipmentStatus;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.entity.NotificationType;
import com.infotact.rstp.entity.Role;
import com.infotact.rstp.repository.BidRepository;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.UserRepository;
import com.infotact.rstp.service.BidService;
import com.infotact.rstp.service.NotificationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BidServiceImpl implements BidService {

    private final BidRepository bidRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BidServiceImpl(BidRepository bidRepository,
            ShipmentRepository shipmentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.bidRepository = bidRepository;
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public BidResponse placeBid(BidRequest request, String carrierEmail) {

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new RuntimeException("Shipment not found with id: " + request.getShipmentId()));

        User carrier = getUserByEmail(carrierEmail);
        if (carrier.getRole() != Role.CARRIER) {
            throw new IllegalArgumentException("Only carriers can place bids");
        }
        if (shipment.getStatus() != ShipmentStatus.OPEN && shipment.getStatus() != ShipmentStatus.BIDDING) {
            throw new IllegalArgumentException("Shipment is not open for bidding");
        }

        Bid bid = Bid.builder()
                .shipment(shipment)
                .carrier(carrier)
                .bidAmount(request.getBidPrice())
                .message(request.getMessage())
                .status(BidStatus.PENDING)
                .build();

        shipment.setStatus(ShipmentStatus.BIDDING);

        Bid savedBid = bidRepository.save(bid);
        shipmentRepository.save(shipment);

        notificationService.createAndBroadcastNotification(
                shipment.getShipper().getId(),
                shipment.getShipmentId(),
                "New bid received from " + carrier.getName()
                        + " for shipment ID: " + shipment.getShipmentId()
                        + " with amount: " + savedBid.getBidAmount(),
                NotificationType.BID_RECEIVED
        );

        return mapToResponse(savedBid);
    }

    @Override
    public List<BidResponse> getBidsByShipment(Long shipmentId, String shipperEmail) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found with id: " + shipmentId));
        assertShipperOwnsShipment(shipment, shipperEmail);

        return bidRepository.findByShipmentShipmentId(shipmentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BidResponse> getBidsByCarrier(String carrierEmail) {
        User carrier = getUserByEmail(carrierEmail);
        if (carrier.getRole() != Role.CARRIER) {
            throw new IllegalArgumentException("Only carriers can view carrier bids");
        }

        return bidRepository.findByCarrierId(carrier.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BidResponse acceptLowestBid(Long shipmentId, String shipperEmail) {

        Bid lowestBid = bidRepository
                .findFirstByShipmentShipmentIdAndStatusOrderByBidAmountAsc(
                        shipmentId,
                        BidStatus.PENDING
                )
                .orElseThrow(() -> new RuntimeException("No pending bids found for shipment id: " + shipmentId));

        assertShipperOwnsShipment(lowestBid.getShipment(), shipperEmail);

        return acceptPendingBid(lowestBid);
    }

    @Override
    @Transactional
    public BidResponse acceptBid(Long shipmentId, Long bidId, String shipperEmail) {
        Bid bidToAccept = bidRepository.findByBidIdAndShipmentShipmentId(bidId, shipmentId)
                .orElseThrow(() -> new RuntimeException("Bid not found for shipment id: " + shipmentId));
        if (bidToAccept.getStatus() != BidStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bids can be accepted");
        }

        assertShipperOwnsShipment(bidToAccept.getShipment(), shipperEmail);

        return acceptPendingBid(bidToAccept);
    }

    private BidResponse acceptPendingBid(Bid acceptedBid) {
        Long shipmentId = acceptedBid.getShipment().getShipmentId();
        List<Bid> allBids = bidRepository.findByShipmentShipmentId(shipmentId);

        for (Bid bid : allBids) {
            if (bid.getBidId().equals(acceptedBid.getBidId())) {
                bid.setStatus(BidStatus.ACCEPTED);
            } else {
                bid.setStatus(BidStatus.REJECTED);
            }
        }

        Shipment shipment = acceptedBid.getShipment();
        shipment.setAwardedCarrier(acceptedBid.getCarrier());
        shipment.setAcceptedBidAmount(acceptedBid.getBidAmount());
        shipment.setStatus(ShipmentStatus.AWAITING_PICKUP);

        bidRepository.saveAll(allBids);
        shipmentRepository.save(shipment);

        notificationService.createAndBroadcastNotification(
                acceptedBid.getCarrier().getId(),
                shipment.getShipmentId(),
                "Congratulations! Your bid has been accepted for shipment ID: "
                        + shipment.getShipmentId(),
                NotificationType.BID_AWARDED
        );

        return mapToResponse(acceptedBid);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void assertShipperOwnsShipment(Shipment shipment, String shipperEmail) {
        User shipper = getUserByEmail(shipperEmail);
        if (shipper.getRole() != Role.SHIPPER || shipment.getShipper() == null ||
                !shipment.getShipper().getId().equals(shipper.getId())) {
            throw new AccessDeniedException("You can only manage bids for your own shipments");
        }
    }

    private BidResponse mapToResponse(Bid bid) {
        return BidResponse.builder()
                .bidId(bid.getBidId())
                .shipmentId(bid.getShipment().getShipmentId())
                .carrierId(bid.getCarrier().getId())
                .carrierName(bid.getCarrier().getName())
                .bidPrice(bid.getBidAmount())
                .status(bid.getStatus())
                .bidTime(bid.getCreatedAt())
                .build();
    }
}
