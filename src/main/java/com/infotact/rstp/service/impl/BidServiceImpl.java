package com.infotact.rstp.service.impl;

import com.infotact.rstp.dto.BidRequest;
import com.infotact.rstp.dto.BidResponse;
import com.infotact.rstp.entity.Bid;
import com.infotact.rstp.entity.BidStatus;
import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.ShipmentStatus;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.repository.BidRepository;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.UserRepository;
import com.infotact.rstp.service.BidService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BidServiceImpl implements BidService {

    private final BidRepository bidRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    public BidServiceImpl(BidRepository bidRepository,
                          ShipmentRepository shipmentRepository,
                          UserRepository userRepository) {
        this.bidRepository = bidRepository;
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BidResponse placeBid(BidRequest request) {

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new RuntimeException("Shipment not found with id: " + request.getShipmentId()));

        User carrier = userRepository.findById(request.getCarrierId())
                .orElseThrow(() -> new RuntimeException("Carrier not found with id: " + request.getCarrierId()));

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

        return mapToResponse(savedBid);
    }

    @Override
    public List<BidResponse> getBidsByShipment(Long shipmentId) {
        return bidRepository.findByShipmentShipmentId(shipmentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BidResponse> getBidsByCarrier(Long carrierId) {
        return bidRepository.findByCarrierId(carrierId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BidResponse acceptLowestBid(Long shipmentId) {

        Bid lowestBid = bidRepository
                .findFirstByShipmentShipmentIdAndStatusOrderByBidAmountAsc(
                        shipmentId,
                        BidStatus.PENDING
                )
                .orElseThrow(() -> new RuntimeException("No pending bids found for shipment id: " + shipmentId));

        List<Bid> allBids = bidRepository.findByShipmentShipmentId(shipmentId);

        for (Bid bid : allBids) {
            if (bid.getBidId().equals(lowestBid.getBidId())) {
                bid.setStatus(BidStatus.ACCEPTED);
            } else {
                bid.setStatus(BidStatus.REJECTED);
            }
        }

        Shipment shipment = lowestBid.getShipment();
        shipment.setAwardedCarrier(lowestBid.getCarrier());
        shipment.setAcceptedBidAmount(lowestBid.getBidAmount());
        shipment.setStatus(ShipmentStatus.AWARDED);

        bidRepository.saveAll(allBids);
        shipmentRepository.save(shipment);

        return mapToResponse(lowestBid);
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
