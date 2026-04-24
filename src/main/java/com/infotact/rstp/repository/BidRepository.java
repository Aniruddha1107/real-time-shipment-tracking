package com.infotact.rstp.repository;

import com.infotact.rstp.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByShipmentShipmentId(Long shipmentId);
    List<Bid> findByCarrierId(Long carrierId);
}
