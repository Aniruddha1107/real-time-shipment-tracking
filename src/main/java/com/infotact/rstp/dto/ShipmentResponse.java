package com.infotact.rstp.dto;

import com.infotact.rstp.entity.ShipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ShipmentResponse {
    private Long shipmentId;
    private String title;
    private String description;
    private String origin;
    private String destination;
    private Double weight;
    private BigDecimal priceExpected;
    private ShipmentStatus status;
    private Long shipperId;
    private String shipperName;
    private Long awardedCarrierId;
    private String awardedCarrierName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
