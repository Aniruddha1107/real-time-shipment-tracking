package com.infotact.rstp.repository;

import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByStatus(ShipmentStatus status);
    List<Shipment> findByShipperId(Long shipperId);
    List<Shipment> findByAwardedCarrierId(Long carrierId);
    List<Shipment> findByStatusIn(List<ShipmentStatus> statuses);
}
