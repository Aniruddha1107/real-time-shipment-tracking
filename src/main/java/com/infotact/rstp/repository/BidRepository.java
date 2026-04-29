package com.infotact.rstp.repository;

import com.infotact.rstp.entity.Bid;
import com.infotact.rstp.entity.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByShipmentShipmentId(Long shipmentId);
    List<Bid> findByCarrierId(Long carrierId);

    Optional<Bid> findFirstByShipmentShipmentIdAndStatusOrderByBidAmountAsc(
            Long shipmentId,
            BidStatus status
    );
}
