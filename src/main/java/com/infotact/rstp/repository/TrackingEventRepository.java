package com.infotact.rstp.repository;

import com.infotact.rstp.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findByShipment_ShipmentIdOrderByEventTimestampDesc(Long shipmentId);
}
