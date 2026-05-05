package com.infotact.rstp.service.impl;

import com.infotact.rstp.dto.BidResponse;
import com.infotact.rstp.entity.*;
import com.infotact.rstp.repository.BidRepository;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.UserRepository;
import com.infotact.rstp.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceImplTest {

    @Mock private BidRepository bidRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private BidServiceImpl bidService;

    @Test
    void acceptLowestBid_ShouldAcceptLowestBidAndRejectOthers() {
        User shipper = User.builder().id(1L).name("Shipper").role(Role.SHIPPER).build();
        User carrier1 = User.builder().id(2L).name("Carrier 1").role(Role.CARRIER).build();
        User carrier2 = User.builder().id(3L).name("Carrier 2").role(Role.CARRIER).build();

        Shipment shipment = Shipment.builder()
                .shipmentId(10L)
                .shipper(shipper)
                .status(ShipmentStatus.BIDDING)
                .build();

        Bid lowestBid = Bid.builder()
                .bidId(100L)
                .shipment(shipment)
                .carrier(carrier1)
                .bidAmount(new BigDecimal("500"))
                .status(BidStatus.PENDING)
                .build();

        Bid higherBid = Bid.builder()
                .bidId(101L)
                .shipment(shipment)
                .carrier(carrier2)
                .bidAmount(new BigDecimal("700"))
                .status(BidStatus.PENDING)
                .build();

        when(bidRepository.findFirstByShipmentShipmentIdAndStatusOrderByBidAmountAsc(10L, BidStatus.PENDING))
                .thenReturn(Optional.of(lowestBid));

        when(bidRepository.findByShipmentShipmentId(10L))
                .thenReturn(List.of(lowestBid, higherBid));

        BidResponse response = bidService.acceptLowestBid(10L);

        assertEquals(BidStatus.ACCEPTED, lowestBid.getStatus());
        assertEquals(BidStatus.REJECTED, higherBid.getStatus());
        assertEquals(ShipmentStatus.AWARDED, shipment.getStatus());
        assertEquals(carrier1, shipment.getAwardedCarrier());
        assertEquals(new BigDecimal("500"), shipment.getAcceptedBidAmount());
        assertEquals(100L, response.getBidId());

        verify(bidRepository).saveAll(List.of(lowestBid, higherBid));
        verify(shipmentRepository).save(shipment);
    }
}
